package com.stanfy.mattock.gradle

import org.gradle.api.Project;

/**
 * Mattock plugin configuration.
 */
public class MattockConfig {

  /** Base output directory name. */
  private static final String BASE_OUT_DIR_NAME = "android-tests"

  /** Test source directories. */
  List<File> testSrcDirs

  File getBaseOutputDir(final Project project) {
    return new File(project.buildDir, BASE_OUT_DIR_NAME)
  }

  File getSourcesOutput(final Project project) {
    return new File(getBaseOutputDir(project), "source")
  }

}
