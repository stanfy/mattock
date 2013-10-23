package com.stanfy.mattock.gradle.test

import com.stanfy.mattock.gradle.AssembleAndroidTestsTask
import com.stanfy.mattock.gradle.MattockConfig
import com.stanfy.mattock.gradle.MattockGradlePlugin
import com.stanfy.mattock.gradle.RunAndroidTestsTask
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

  def "should add androidTest task"() {
    given:
    project.apply plugin: 'java'
    project.apply plugin: 'mattock'
    RunAndroidTestsTask task = project.tasks.androidTest as RunAndroidTestsTask

    expect:
    task != null
    !task.testApk != null
    task.packageName != null
  }

  def "should respect mattock config when creates tasks"() {
    given:
    MattockGradlePlugin plugin = new MattockGradlePlugin()
    project.apply plugin: 'java'
    plugin.apply(project)
    project.test {
      include '**/A'
      exclude 'B'
    }
    project.mattock {
      debug true
      allDevices true
      ignoreMissingDevices true
      devices = ['a', 'b']
      testSrcDirs = [project.file('a')]
    }
    plugin.onConfigReady(project)
    AssembleAndroidTestsTask assemble = project.tasks.assembleAndroidTests as AssembleAndroidTestsTask
    RunAndroidTestsTask run = project.tasks.androidTest as RunAndroidTestsTask

    expect:
    assemble.includes.contains('**/A')
    assemble.excludes.contains('B')
    assemble.testSrcDirs.collect { it }[0].name == 'a'
    run.debug
    run.allDevices
    run.ignoreMissingDevices
    run.devices.contains('a')
    run.devices.contains('b')
  }

}
