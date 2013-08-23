package com.stanfy.mattock;

import android.content.Context;
import android.util.Log;

import org.apache.maven.plugin.surefire.report.StatelessXmlReporter;
import org.apache.maven.plugin.surefire.report.TestSetRunListener;
import org.junit.runner.JUnitCore;

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

  public static void run(final Context context, final Collection<Class<?>> testClasses) {
    for (Class<?> test : testClasses) {
      if (Modifier.isAbstract(test.getModifiers())) { continue; }
      StatelessXmlReporter xmlReporter = new StatelessXmlReporter(context.getFilesDir(), null, false);
      TestSetRunListener surefireListener = new TestSetRunListener(null, null, xmlReporter, new DummyTestcycleConsoleOutputReceiver(), null, null, false, false, false);
      AndroidJUnit4RunListener junitListener = new AndroidJUnit4RunListener(test, surefireListener);
      JUnitCore core = new JUnitCore();
      core.addListener(junitListener);
      Log.i("Mattock", "Running test " + test);
      core.run(test);
    }
  }

}
