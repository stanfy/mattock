package com.stanfy.mattock;

import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.util.Log;

import org.apache.maven.plugin.surefire.report.StatelessXmlReporter;
import org.apache.maven.plugin.surefire.report.TestSetRunListener;
import org.apache.maven.surefire.report.RunStatistics;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;

import java.io.File;
import java.io.IOException;
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

  @SuppressWarnings("deprecation")
  public static File getReportsDir(final Context context) {
    return context.getDir("test-reports", Context.MODE_WORLD_READABLE);
  }

  @SuppressWarnings("ResultOfMethodCallIgnored")
  public static void run(final Context context, final Collection<Class<?>> testClasses) {
    File reportsDir = getReportsDir(context);
    reportsDir.mkdirs();

    File[] oldReports = reportsDir.listFiles();
    if (oldReports != null) {
      Log.i("Mattock", "Cleaning old reports");
      for (File oldReport : oldReports) {
        if (!oldReport.delete()) {
          Log.w("Mattock", "Cannot delete " + oldReport);
        }
      }
    }

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

    File[] reports = reportsDir.listFiles();
    if (reports == null) {
      throw new IllegalStateException("No reports found");
    }
    for (File r : reports) {
      setReadable(r);
    }
  }

  private static void setReadable(final File f) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
      if (!f.setReadable(true, false)) {
        throw new RuntimeException("Cannot make " + f + "readable");
      }
    } else {
      try {
        Runtime.getRuntime().exec(new String[] {"chmod", "777", f.getAbsolutePath()});
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  }

}
