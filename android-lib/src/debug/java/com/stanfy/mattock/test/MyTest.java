package com.stanfy.mattock.test;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

public class MyTest {

  @Test
  public void test1() {
    Assert.assertEquals(true, true);
  }

  @Test
  public void test2() {
    Assert.assertEquals(true, false);
  }

  @Ignore
  @Test
  public void test3() {
    Assert.assertEquals(true, false);
  }

}
