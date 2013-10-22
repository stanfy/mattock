package com.stanfy.mattock;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import org.apache.maven.plugin.surefire.report.StatelessXmlReporter;
import org.apache.maven.plugin.surefire.report.TestSetRunListener;
import org.apache.maven.surefire.report.RunStatistics;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;

import java.io.File;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collection;

/**
 * Utilities for running JUnit4 tests on Android.
 */
public final class Mattock {

  private Mattock() { /* nothing */ }

  public static void run(final Context context, final Class<?>... testClasses) {
    run(context, Arrays.asList(testClasses));
  }

  public static File getReportsDir(final Context context) {
    return context.getFilesDir();
  }

  public static void run(final Context context, final Collection<Class<?>> testClasses) {
    File reportsDir = getReportsDir(context);
    reportsDir.mkdirs();
    for (Class<?> test : testClasses) {
      if (Modifier.isAbstract(test.getModifiers())) { continue; }
      StatelessXmlReporter xmlReporter = new StatelessXmlReporter(reportsDir, null, false);
      TestSetRunListener surefireListener = new TestSetRunListener(null, null, xmlReporter, new DummyTestcycleConsoleOutputReceiver(), null, new RunStatistics(), false, false, false);
      AndroidJUnit4RunListener junitListener = new AndroidJUnit4RunListener(test, surefireListener);
      JUnitCore core = new JUnitCore();
      core.addListener(junitListener);
      Log.i("Mattock", "Running test " + test);
      Result res = core.run(test);
      Log.i("Mattock", "Run: " + res.getRunCount() + ". Failures: " + res.getFailureCount() + ". Ignored: " + res.getIgnoreCount() + ".");
      if (res.getFailureCount() > 0) {
        Log.e("Mattock", "First error", res.getFailures().get(0).getException());
      }
    }
  }

}
