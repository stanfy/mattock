package com.stanfy.mattock;

import org.apache.maven.plugin.surefire.report.TestSetRunListener;
import org.apache.maven.surefire.common.junit4.JUnit4RunListener;
import org.apache.maven.surefire.report.ReportEntry;
import org.apache.maven.surefire.report.SimpleReportEntry;
import org.junit.runner.Description;
import org.junit.runner.Result;

/** Run listener for Android. */
class AndroidJUnit4RunListener extends JUnit4RunListener {

  /** Test class. */
  private final Class<?> testClass;
  /** Surefire listener. */
  private final TestSetRunListener surefireListener;
  /** Test set report entry. */
  private ReportEntry entry;

  public AndroidJUnit4RunListener(final Class<?> clazz, final TestSetRunListener surefireListener) {
    super(surefireListener);
    this.testClass = clazz;
    this.surefireListener = surefireListener;
  }

  @Override
  public void testRunStarted(final Description description) throws Exception {
    this.entry = new SimpleReportEntry(testClass.getName(), testClass.getName());
    surefireListener.testSetStarting(entry);
  }

  @Override
  public void testRunFinished(final Result result) throws Exception {
    surefireListener.testSetCompleted(entry);
  }
}
