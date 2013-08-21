package com.stanfy.mattock.gradle.test

import com.stanfy.mattock.gradle.AssembleAndroidTestsTask
import com.stanfy.mattock.gradle.MattockConfig
import com.stanfy.mattock.gradle.MattockGradlePlugin
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Specification

/**
 * Tests for MattockGradlePlugin.
 */
public class MattockGradlePluginSpec extends Specification {

  /** Project under test. */
  Project project = ProjectBuilder.builder().build();

  def "should fail without Java plugin"() {
    when:
    project.apply plugin: 'mattock'

    then:
    thrown(IllegalStateException.class)
  }

  def "should work with Java plugin"() {
    given:
    project.apply plugin: 'java'
    project.apply plugin: 'mattock'

    expect:
    project.plugins.hasPlugin(MattockGradlePlugin.class)
  }

  def "should add mattock extension"() {
    given:
    project.apply plugin: 'java'
    project.apply plugin: 'mattock'

    expect:
    project.mattock instanceof MattockConfig
  }

  def "should add assembleAndroidTests task"() {
    given:
    project.apply plugin: 'java'
    project.apply plugin: 'mattock'
    AssembleAndroidTestsTask task = project.tasks['assembleAndroidTests'] as AssembleAndroidTestsTask

    expect:
    task != null
    !task.testSrcDirs.empty
    task.outputDir != null
  }

}
