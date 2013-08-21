package com.stanfy.mattock.gradle.test

import com.stanfy.mattock.gradle.AssembleAndroidTestsTask;

import static org.fest.assertions.api.Assertions.*;

import org.gradle.api.Project;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.Test;

/**
 * Tests for MattockGradlePlugin.
 */
public class MattockGradlePluginTest {

  // TODO: use spock

  @Test(expected = IllegalStateException.class)
  void shouldFailWithoutJavaPlugin() {
    Project project = ProjectBuilder.builder().build();
    project.apply plugin: 'mattock'
  }

  @Test
  void shouldWorkWithJavaPlugin() {
    Project project = ProjectBuilder.builder().build();
    project.apply plugin: 'java'
    project.apply plugin: 'mattock'
  }

  @Test
  void shouldAddAssembleAndroidTestsTask() {
    Project project = ProjectBuilder.builder().build();
    project.apply plugin: 'java'
    project.apply plugin: 'mattock'

    AssembleAndroidTestsTask task = project.tasks['assembleAndroidTests']
    assertThat(task) isNotNull()
    assertThat(task.testSrcDirs) isNotEmpty()
  }


}
