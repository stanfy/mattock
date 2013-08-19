package com.stanfy.mattock;

import android.app.IntentService;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Service that runs tests specified in meta data.
 */
public class MattockService extends IntentService {

  /** Meta data kay for test class names. */
  public static final String META_TESTS = "com.stanfy.mattock.TEST_CLASSES";

  /** Test classes. */
  private ArrayList<Class<?>> testClasses;

  public MattockService() {
    super("mattock");
  }

  public ArrayList<Class<?>> getTestClasses() {
    if (testClasses == null) {
      try {

        String namesString = getPackageManager().getApplicationInfo(getPackageName(), PackageManager.GET_META_DATA).metaData.getString(META_TESTS);
        if (TextUtils.isEmpty(namesString)) {
          throw new IllegalStateException("Application meta data <" + META_TESTS + "> is empty");
        }

        String[] tests = namesString.split(";");
        ArrayList<Class<?>> classes = new ArrayList<Class<?>>(tests.length);
        for (String test : tests) {
          classes.add(Class.forName(test));
        }

        testClasses = classes;
      } catch (PackageManager.NameNotFoundException e) {
        throw new RuntimeException(e);
      } catch (ClassNotFoundException e) {
        throw new RuntimeException("Cannot load test class", e);
      }
    }

    return testClasses;
  }

  @Override
  protected void onHandleIntent(final Intent intent) {
    Mattock.run(this, getTestClasses());
  }

}