package com.stanfy.mattock.gradle;

import org.gradle.api.Plugin;
import org.gradle.api.Project
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.GradleBuild
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.testing.Test
import org.slf4j.Logger
import org.slf4j.LoggerFactory;

/**
 * Gradle plugin that integrates Mattock - JUnit4 tests runner for Android.
 */
class MattockGradlePlugin implements Plugin<Project> {

  /** Logger. */
  private static final Logger LOG = LoggerFactory.getLogger(MattockGradlePlugin.class);

  /** Assemble task. */
  private AssembleAndroidTestsTask assembleTask
  /** Build test apk task. */
  private GradleBuild buildTestsTask
  /** Run tests task. */
  private RunAndroidTestsTask runTask

  @Override
  public void apply(final Project project) {
    if (!(project.plugins.hasPlugin(JavaPlugin.class))) {
      throw new IllegalStateException("Currently mattock can be used with java projects only to test them on Android devices");
    }

    //noinspection GroovyAssignabilityCheck
    project.extensions.add("mattock", MattockConfig)
    MattockConfig config = project.mattock

    final def testTask = project.tasks.findByName('test')
    if (!testTask || !(testTask instanceof Test)) {
      throw new IllegalArgumentException("Cannot find Test task with name 'test'");
    }
    final def jarTask = project.tasks.findByName('jar')
    if (!jarTask || !(jarTask instanceof Jar)) {
      throw new IllegalArgumentException("Cannot find Jar task with name 'jar'");
    }

    List<File> testDirs = testTask.testSrcDirs
    File mainJar = jarTask.archivePath

    // assemble tests
    assembleTask = project.tasks.create("assembleAndroidTests", AssembleAndroidTestsTask.class)
    assembleTask.group = BasePlugin.BUILD_GROUP
    assembleTask.description = "Assembles Android project that can build a service for further installation and running tests on a device"
    assembleTask.testSrcDirs = testDirs;
    assembleTask.outputDir = config.getSourcesOutput(project)
    assembleTask.mainJar = mainJar

    assembleTask.dependsOn 'jar'

    // build tests
    buildTestsTask = project.tasks.create("buildAndroidTestApk", GradleBuild.class)
    buildTestsTask.group = BasePlugin.BUILD_GROUP
    buildTestsTask.description = "Builds APK file that will be installed on a device to run tests"
    buildTestsTask.tasks = ['assembleDebug']

    buildTestsTask.dependsOn assembleTask

    // run tests
    runTask = project.tasks.create("androidTest", RunAndroidTestsTask.class)
    runTask.group = JavaBasePlugin.VERIFICATION_GROUP
    runTask.description = "Run tests on Android device"

    runTask.dependsOn buildTestsTask

    syncTaskOutputs(project)

    project.afterEvaluate {
      onConfigReady(project)
    }

  }

  void onConfigReady(final Project project) {
    MattockConfig config = project.mattock
    final def testTask = project.tasks.findByName('test')
    final def jarTask = project.tasks.findByName('jar')

    if (config.testSrcDirs) {
      assembleTask.testSrcDirs = config.testSrcDirs
    }
    assembleTask.includes = testTask.includes
    assembleTask.excludes = testTask.excludes
    assembleTask.mainJar = jarTask.archivePath
    LOG.debug("Configured includes: " + assembleTask.includes)
    LOG.debug("Configured excludes: " + assembleTask.excludes)
    syncTaskOutputs(project)

    runTask.debug = config.debug
    runTask.allDevices = config.allDevices
    runTask.devices = config.devices
    runTask.ignoreMissingDevices = config.ignoreMissingDevices
  }

  private void syncTaskOutputs(final Project project) {
    assembleTask.testResourcesDir = Collections.singletonList(new File(project.projectDir, "src/test/resources"))

    buildTestsTask.dir = assembleTask.mainOutputDirectory
    buildTestsTask.buildFile = new File(buildTestsTask.dir, "build.gradle")

    runTask.testApk = assembleTask.debugApk
    runTask.packageName = assembleTask.packageName
    runTask.reportsDir = new File(project.buildDir, "android-tests/reports")
  }

}
