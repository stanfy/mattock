package com.stanfy.mattock.gradle

import org.apache.tools.ant.taskdefs.condition.Os
import org.gradle.api.DefaultTask
import org.gradle.api.internal.tasks.testing.junit.report.DefaultTestReport
import org.gradle.api.tasks.TaskAction
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Task for running assembled Android tests.
 */
class RunAndroidTestsTask extends DefaultTask {

  /** Logger. */
  private static final Logger LOG = LoggerFactory.getLogger(RunAndroidTestsTask.class)

  /** APK file that should be tested. */
  File testApk

  /** Tests package name. */
  String packageName

  /** Output directory. */
  File reportsDir

  @TaskAction
  void runTests() {

    String androidHome = System.env['ANDROID_HOME']
    if (!androidHome) {
      throw new IllegalStateException("Cannot resolve Android SDK path. Set ANDROID_HOME env variable.")
    }
    File androidSdk = new File(androidHome)
    if (!androidSdk.exists()) {
      throw new IllegalStateException("Android SDK path does not exist: $androidSdk")
    }

    String ext = Os.isFamily(Os.FAMILY_WINDOWS) ? ".exe" : ""
    File adb = new File(androidSdk, "platform-tools/adb$ext")
    if (!adb.exists()) {
      throw new IllegalStateException("Cannot find ADB executable at path $adb")
    }

    println "Installing test APK on a device..."
    runCmd "$adb install -r $testApk"

    ServerSocket server = new ServerSocket(9999);
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

    println "Running tests..."
    runCmd "$adb shell am startservice -n $packageName/com.stanfy.mattock.MattockService --es receiverAddress $host --ei receiverPort 9999"

    println "Getting results..."

    Socket socket = server.accept()
    BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"))
    String reportsPath = input.readLine()
    input.close()
    server.close()

    def files = runCmd("$adb shell ls $reportsPath").split(/\n/)
    reportsDir.mkdirs()
    files.each {
      runCmd "$adb pull $reportsPath/$it $reportsDir/$it"
    }
  }

  private static String runCmd(final String cmd) {
    def proc = cmd.execute()
    int res = proc.waitFor()
    if (res != 0) {
      throw new IllegalStateException("Cannot run $cmd.\n${proc.inputStream.text}\n${proc.errorStream.text}")
    }
    return proc.inputStream.text + "\n" + proc.errorStream.text
  }

}
