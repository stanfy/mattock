package com.stanfy.mattock.gradle;

import org.gradle.api.Plugin;
import org.gradle.api.Project
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.testing.Test
import org.slf4j.Logger
import org.slf4j.LoggerFactory;

/**
 * Gradle plugin that integrates Mattock - JUnit4 tests runner for Android.
 */
class MattockGradlePlugin implements Plugin<Project> {

  /** Logger. */
  private static final Logger LOG = LoggerFactory.getLogger(MattockGradlePlugin.class);

  @Override
  public void apply(final Project project) {
    if (!(project.plugins.hasPlugin(JavaPlugin.class))) {
      throw new IllegalStateException("Currently mattock can be used with java projects only to test them on Android devices");
    }

    //noinspection GroovyAssignabilityCheck
    project.extensions.add("mattock", MattockConfig)
    MattockConfig config = project.mattock

    def testTask = project.tasks.findByName('test')
    if (!testTask || !(testTask instanceof Test)) {
      throw new IllegalArgumentException("Cannot find Test task with name 'test'");
    }

    List<File> testDirs = testTask.testSrcDirs

    AssembleAndroidTestsTask assembleTask = project.tasks.create("assembleAndroidTests", AssembleAndroidTestsTask.class)
    assembleTask.group = BasePlugin.BUILD_GROUP;
    assembleTask.description = "Assembles Android project that can build a service for further installation and running on a device"
    assembleTask.testSrcDirs = testDirs;
    assembleTask.outputDir = config.getSourcesOutput(project)

    project.afterEvaluate {
      if (config.testSrcDirs) {
        assembleTask.testSrcDirs = config.testSrcDirs
      }
    }

  }

}
