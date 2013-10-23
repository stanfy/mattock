package com.stanfy.mattock.gradle

import com.android.ddmlib.*
import org.apache.tools.ant.taskdefs.condition.Os
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.lang.reflect.Constructor
import java.lang.reflect.Field
import java.lang.reflect.InvocationTargetException
/**
 * Task for running assembled Android tests.
 */
class RunAndroidTestsTask extends DefaultTask {

  /** Logger. */
  private static final Logger LOG = LoggerFactory.getLogger(RunAndroidTestsTask.class)

  /** APK file that should be tested. */
  @InputFile
  File testApk

  /** Tests package name. */
  @Input
  String packageName

  /** Output directory. */
  @OutputDirectory
  File reportsDir

  boolean ignoreMissingDevices

  boolean debug

  boolean allDevices

  Set<String> devices

  /** Turn on debug logging in ddmlib classes. */
  static void setDdmlibInternalLoggingLevel() {
    try {
      Field level = Log.class.getDeclaredField("mLevel");
      level.setAccessible(true);
      level.set(Log.class, Log.LogLevel.DEBUG);
    } catch (NoSuchFieldException ignored) {
    } catch (IllegalAccessException ignored) {
    }
  }

  private static AndroidDebugBridge initAdb(File adbPath) {
    AndroidDebugBridge.init(false)
    AndroidDebugBridge adb = AndroidDebugBridge.createBridge(adbPath.absolutePath, true)

    // wait for adb
    for (int i = 1; i < 10; i++) {
      try {
        Thread.sleep(i * 100);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
      if (adb.isConnected()) {
        return adb;
      }
    }
    throw new RuntimeException("Unable to connect to adb.");
  }

  /** Get a {@link FileListingService.FileEntry} for an arbitrary path. */
  static FileListingService.FileEntry obtainDirectoryFileEntry(String path) {
    try {
      Constructor<FileListingService.FileEntry> c =
          FileListingService.FileEntry.class.getDeclaredConstructor(FileListingService.FileEntry.class,
              String.class, int.class, boolean.class);
      c.setAccessible(true);
      return (FileListingService.FileEntry) path.split('/').inject(null) { FileListingService.FileEntry entry, String part ->
        c.newInstance(entry, part, FileListingService.TYPE_DIRECTORY, entry == null);
      }
    } catch (NoSuchMethodException ignored) {
    } catch (InvocationTargetException ignored) {
    } catch (InstantiationException ignored) {
    } catch (IllegalAccessException ignored) {
    }
    return null;
  }

  private static File getAdbPath(final File androidHome) {
    String ext = Os.isFamily(Os.FAMILY_WINDOWS) ? ".exe" : ""
    File adbPath = new File(androidHome, "platform-tools/adb$ext")
    if (!adbPath.exists()) {
      throw new IllegalStateException("Cannot find ADB executable at path $adbPath")
    }
    return adbPath
  }

  @TaskAction
  void runTests() {

    File androidHome = project.mattock.androidSdk
    if (!androidHome?.exists()) {
      String androidHomeStr = System.env['ANDROID_HOME']
      if (androidHomeStr) {
        androidHome = new File(androidHomeStr)
      }
    }
    if (!androidHome) {
      throw new IllegalStateException("Cannot resolve Android SDK path. Set either ANDROID_HOME env variable or androidSdk in mattock.")
    }
    if (!androidHome.exists()) {
      throw new IllegalStateException("Android SDK path does not exist: $androidHome")
    }

    if (debug) {
      setDdmlibInternalLoggingLevel();
    }

    File adbPath = getAdbPath(androidHome)
    AndroidDebugBridge adb = initAdb(adbPath)

    try {
      def connectedDevices = adb.devices as List
      def connectedDeviceNames = connectedDevices.collect { it.serialNumber }
      if (!devices && allDevices) {
        devices = connectedDeviceNames
      }
      if (!devices) {
        String msg = "No devices set to run. Connected devices: $connectedDeviceNames."
        if (ignoreMissingDevices) {
          LOG.error(msg)
          return
        } else {
          throw new IllegalStateException(msg);
        }
      }

      connectedDevices.findAll { IDevice d -> connectedDeviceNames.contains(d.serialNumber) } .each { IDevice device ->
        runOnADevice(adbPath, adb, device)
      }
    } finally {
      AndroidDebugBridge.terminate()
    }

    println devices ? "Run on ${devices.size()} devices" : "No connected devices found"
  }

  private static String runCmd(final String cmd) {
    def process = cmd.execute()
    int res = process.waitFor()
    if (res != 0) {
      throw new IllegalStateException("Cannot run $cmd.\n${process.inputStream.text}\n${process.errorStream.text}")
    }
    return process.inputStream.text + "\n" + process.errorStream.text
  }

  void runOnADevice(final File adbPath, final AndroidDebugBridge adb, final IDevice device) {
    LOG.info("Installing APK on $device")
    String installError = device.installPackage(testApk.absolutePath, true)
    if (installError) {
      LOG.error("Failed to install on $device.serialNumber: $installError")
      return
    }

    int port = 9999
    ServerSocket server = new ServerSocket(port);
    def interfaces = NetworkInterface.getNetworkInterfaces()
    def host = null
    while (interfaces.hasMoreElements()) {
      NetworkInterface ni = interfaces.nextElement()
      def addresses = ni.inetAddresses
      while (addresses.hasMoreElements()) {
        def address = addresses.nextElement().hostAddress
        if (!address.startsWith("127") && address != "localhost" && !address.contains(':')) {
          host = address
          break
        }
      }
    }

    // TODO: find a way how to run service with ddmlib
    LOG.info("Running tests on $device")
    runCmd "$adbPath shell am startservice -n $packageName/com.stanfy.mattock.MattockService --es receiverAddress $host --ei receiverPort $port"

    LOG.info("Waiting for results from $device")

    Socket socket = server.accept()
    BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"))
    String reportsPath = input.readLine()
    input.close()
    server.close()

    File out = new File(reportsDir, device.serialNumber)
    out.mkdirs()
    FileListingService.FileEntry deviceDir = obtainDirectoryFileEntry(reportsPath)
    LOG.info "Pulling reports from $device.serialNumber ($deviceDir.fullPath, $deviceDir.directory) to $out"
    device.syncService.pull([deviceDir] as FileListingService.FileEntry[],
        out.absolutePath, SyncService.nullProgressMonitor)
    fixOutput(out)

    device.uninstallPackage(packageName)
  }

  private static void fixOutput(final File out) {
    File[] content = out.listFiles()
    if (!content) {
      throw new RuntimeException("No reports!")
    }
    if (content.length == 1 && content[0].directory) {
      File[] reports = content[0].listFiles()
      if (reports && reports.length > 0) {
        boolean ok = true
        reports.each {
          ok &= it.renameTo(new File(it.parentFile.parentFile, it.name))
        }
        if (ok) {
          content[0].delete()
        }
      }
    }
  }

}
