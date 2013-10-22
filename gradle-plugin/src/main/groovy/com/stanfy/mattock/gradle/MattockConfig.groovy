package com.stanfy.mattock.gradle

import org.gradle.api.Project;

/**
 * Mattock plugin configuration.
 */
public class MattockConfig {

  /** Mattock version. */
  public static final String VERSION = "1.0-SNAPSHOT";

  /** Base output directory name. */
  private static final String BASE_OUT_DIR_NAME = "android-tests"

  /** Debug logging flag. */
  boolean debug

  /** Run on all the devices. */
  boolean allDevices

  /** Flag to ignore errors with devices. */
  boolean ignoreMissingDevices

  /** Devices to run on. */
  Set<String> devices

  /** Android SDK location. */
  File androidSdk

  /** Test source directories. */
  List<File> testSrcDirs

  File getBaseOutputDir(final Project project) {
    return new File(project.buildDir, BASE_OUT_DIR_NAME)
  }

  File getSourcesOutput(final Project project) {
    return new File(getBaseOutputDir(project), "source")
  }

}
