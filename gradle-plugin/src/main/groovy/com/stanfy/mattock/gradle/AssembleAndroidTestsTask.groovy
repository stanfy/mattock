package com.stanfy.mattock.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

/**
 * Task for assembling Android tests. It creates a project that builds APK containing
 * Android service that can run specified tests.
 */
class AssembleAndroidTestsTask extends DefaultTask {

  /** Test sources. */
  @InputFiles
  List<File> testSrcDirs;

  /** Output directory. */
  @OutputDirectory
  File outputDir;

  @TaskAction
  void assmebleAndroidProject() {

  }

}
