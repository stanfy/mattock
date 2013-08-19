package com.stanfy.mattock.test;

import android.content.Intent;
import android.test.ServiceTestCase;

import com.stanfy.mattock.MattockService;

import java.io.File;
import java.io.FilenameFilter;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Tests for MattockService.
 */
public class MattockServiceTest extends ServiceTestCase<MattockServiceTest.Service> {

  public MattockServiceTest() {
    super(Service.class);
  }

  public void testTestsRunning() {
    startService(new Intent(getContext(), MattockService.class));
    try {
      Service.SYNC.await(3, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      throw new RuntimeException("Service does not finish correctly");
    }
    String[] reports = getContext().getFilesDir().list(new FilenameFilter() {
      @Override
      public boolean accept(File dir, String filename) {
        return filename.startsWith("TEST-");
      }
    });
    assertEquals(2, reports.length);
    assertTrue(reports[0].indexOf("MyTest.xml") > 0);
    assertTrue(reports[1].indexOf("MyTest2.xml") > 0);
  }

  /** Service with synchronization. */
  public static class Service extends MattockService {

    static final CountDownLatch SYNC = new CountDownLatch(1);

    @Override
    protected void onHandleIntent(final Intent intent) {
      super.onHandleIntent(intent);
      SYNC.countDown();
    }
  }

}
