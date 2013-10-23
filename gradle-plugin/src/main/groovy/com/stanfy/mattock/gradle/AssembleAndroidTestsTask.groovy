package com.stanfy.mattock.gradle

import org.apache.tools.ant.DirectoryScanner
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Task for assembling Android tests. It creates a project that builds APK containing
 * Android service that can run specified tests.
 */
class AssembleAndroidTestsTask extends DefaultTask {

  /** Logger. */
  private static final Logger LOG = LoggerFactory.getLogger(AssembleAndroidTestsTask.class)

  /** Test sources. */
  @InputFiles
  List<File> testSrcDirs

  /** Test resources. */
  @InputFiles
  List<File> testResourcesDir

  /** Main lib jar. */
  @InputFile
  File mainJar

  /** Output directory. */
  @OutputDirectory
  File outputDir;

  /** Include patterns. */
  Set<String> includes;

  /** Exclude pattern. */
  Set<String> excludes;

  String getTestListDescription() {
    List<String> testsList = []
    DirectoryScanner scanner = new DirectoryScanner()
    if (includes) {
      scanner.includes = includes.toArray(new String[includes.size()])
    }
    if (excludes) {
      scanner.excludes = excludes.toArray(new String[excludes.size()])
    }
    scanner.caseSensitive = true
    testSrcDirs.each {
      scanner.basedir = it
      scanner.scan()
      testsList += scanner.includedFiles as List
    }
    LOG.debug("Included tests: " + testsList)

    String testsDescription = testsList.collect {
      int extIndex = it.lastIndexOf(".")
      extIndex = extIndex == -1 ? -1 : extIndex - 1
      return it[0..extIndex].replaceAll(/\//, ".")
    }.join(";")
    LOG.debug("Test description: " + testsDescription)

    return testsDescription
  }

  File getMainOutputDirectory() {
    return new File(outputDir, "$project.name-tests")
  }

  File getDebugApk() {
    return new File(mainOutputDirectory, "build/apk/$project.name-tests-debug-unaligned.apk")
  }

  String getPackageName() {
    return "${project.group}.${project.name.replaceAll(/\W/, '')}"
  }

  private static void write(final File out, final String what) {
    out.withOutputStream {
      it << what.trim().getBytes('UTF-8')
    }
  }

  @TaskAction
  void assmebleAndroidProject() {
    int minSdkVersion = 7

    File mainDir = getMainOutputDirectory()
    File depsDir = new File(outputDir, "$project.name-deps")

    // make a root project
    write new File(outputDir, "build.gradle"), ""

    write new File(outputDir, "settings.gradle"), """
include '$project.name-deps'
include '$project.name-tests'
"""

    // make a dependencies projects (Android plugin workaround)

    File depManifestFile = new File(depsDir, "src/main/AndroidManifest.xml")
    depManifestFile.parentFile.mkdirs()
    write depManifestFile, """
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
          package="mattock.dependencies"
    android:versionCode="1"
    android:versionName="1" >

  <application />

</manifest>
"""

    write new File(depsDir, "build.gradle"), """
buildscript {
  repositories {
    mavenCentral()
  }
  dependencies {
    classpath 'com.android.tools.build:gradle:0.5.+'
  }
}

apply plugin: 'android-library'

android {
  compileSdkVersion 18
  buildToolsVersion "18.1.0"

  defaultConfig {
    minSdkVersion $minSdkVersion
    targetSdkVersion 18
  }
}

repositories {
  mavenCentral()
  maven { url "https://oss.sonatype.org/content/repositories/snapshots/" }
}

dependencies {
  ${
      ['compile', 'testCompile'].inject("") { res, conf ->
        res + project.configurations[conf].inject("") { confRes, dep ->
          def str = "$dep";
          confRes + (['hamcrest-core', 'xmlpull'].any {str.contains(it)} ? "" : "compile files('$str')\n")
        }
      }
  }
  compile files('$mainJar.absolutePath')

  compile('com.stanfy.mattock:android-lib:${MattockConfig.VERSION}') {
    transitive = false
  }
  compile('com.stanfy.mattock:android-lib-dep:${MattockConfig.VERSION}') {
    transitive = false
  }
  compile('org.apache.maven.surefire:common-junit4:2.15') {
    transitive = false
  }
  compile('org.apache.maven.surefire:surefire-api:2.15') {
    transitive = false
  }
}
"""

    // main test project

    File sourcesOutput = new File(mainDir, "src/main/java")
    File resourcesOutput = new File(mainDir, "src/main/resources")
    sourcesOutput.mkdirs()
    resourcesOutput.mkdirs()

    // copy test sources
    project.ant.copy(todir : sourcesOutput) {
      testSrcDirs.each {
        fileset dir : it
      }
    }
    // copy test resources
    project.ant.copy(todir : resourcesOutput) {
      testResourcesDir.each {
        if (it.exists()) {
          fileset dir : it
        }
      }
    }

    write new File(mainDir, "src/main/AndroidManifest.xml"), """
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
          xmlns:tools="http://schemas.android.com/tools"
          package="$packageName"
    android:versionCode="1"
    android:versionName="1" >

  <application>

    <service
        android:name="com.stanfy.mattock.MattockService"
        android:exported="true" />
    <meta-data
        android:name="com.stanfy.mattock.TEST_CLASSES"
        android:value="$testListDescription"
        />

  </application>

</manifest>
"""

    write new File(mainDir, "build.gradle"), """
buildscript {
  repositories {
    mavenCentral()
  }
  dependencies {
    classpath 'com.android.tools.build:gradle:0.5.+'
  }
}

apply plugin: 'android'

android {
  compileSdkVersion 18
  buildToolsVersion "18.1.0"

  defaultConfig {
    minSdkVersion $minSdkVersion
    targetSdkVersion 18
  }
}

repositories {
  mavenCentral()
  maven { url "https://oss.sonatype.org/content/repositories/snapshots/" }
}

dependencies {
  compile project(':$project.name-deps')
}
"""


  }

}
