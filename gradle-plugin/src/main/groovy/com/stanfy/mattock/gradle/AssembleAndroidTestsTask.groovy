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
  List<File> testSrcDirs;

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

  private static void write(final File out, final String what) {
    out.withOutputStream {
      it << what.trim().getBytes('UTF-8')
    }
  }

  @TaskAction
  void assmebleAndroidProject() {
    File mainDir = new File(outputDir, "$project.name-tests")
    File depsDir = new File(outputDir, "$project.name-deps")

    def depManifest = """
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
          package="mattock.dependencies"
    android:versionCode="1"
    android:versionName="1" >

  <application />

</manifest>
"""
    File depManifestFile = new File(depsDir, "src/main/AndroidManifest.xml")
    depManifestFile.parentFile.mkdirs()
    write depManifestFile, depManifest


    File sourcesOutput = new File(mainDir, "src/main/java")
    sourcesOutput.mkdirs()

    // copy test sources
    project.ant.copy(todir : sourcesOutput) {
      testSrcDirs.each {
        fileset dir : it
      }
    }

    // generate Manifest file
    def manifest = """
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
          xmlns:tools="http://schemas.android.com/tools"
          package="${project.group}.${project.name.replaceAll(/\W/, '')}"
    android:versionCode="1"
    android:versionName="1" >

  <application>

    <service
        android:name=".MattockService"
        android:exported="true" />
    <meta-data
        android:name="com.stanfy.mattock.TEST_CLASSES"
        android:value="$testListDescription"
        />

  </application>

</manifest>
"""
    write new File(outputDir, "src/main/AndroidManifest.xml"), manifest

    // generate build.gradle
    def buildFile = """
buildscript {
  repositories {
    mavenCentral()
  }
  dependencies {
    classpath 'com.android.tools.build:gradle:0.5.+'
  }
}
apply plugin: 'android-library'

repositories {
  mavenCentral()
  maven { url "https://oss.sonatype.org/content/repositories/snapshots/" }
}

android {
  compileSdkVersion 18
  buildToolsVersion "18.0.1"

  defaultConfig {
    minSdkVersion 7
    targetSdkVersion 18
  }
}

dependencies {
  ${
    ['compile', 'testCompile'].inject("") { res, conf ->
      res + project.configurations[conf].inject("") { confRes, dep -> confRes + "compile files('${dep}')\n" }
    }
  }
  compile files('$mainJar.absolutePath')

  compile 'com.stanfy.mattock:android-lib:0.9-SNAPSHOT'
}

"""
    new File(outputDir, "build.gradle").withOutputStream {
      it << buildFile.trim().getBytes('UTF-8')
    }


  }

}
