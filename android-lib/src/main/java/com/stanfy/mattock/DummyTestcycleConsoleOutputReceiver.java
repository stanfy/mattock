package com.stanfy.mattock;

import org.apache.maven.plugin.surefire.report.TestcycleConsoleOutputReceiver;
import org.apache.maven.surefire.report.ReportEntry;

/**
* Receiver that does nothing.
*/
final class DummyTestcycleConsoleOutputReceiver implements TestcycleConsoleOutputReceiver {
  @Override
  public void testSetStarting(final ReportEntry reportEntry) {

  }

  @Override
  public void testSetCompleted(final ReportEntry reportEntry) {

  }

  @Override
  public void close() {

  }

  @Override
  public void writeTestOutput(final byte[] bytes, final int i, final int i2, final boolean b) {

  }
}
