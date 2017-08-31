package com.dtcc.ecd.awslogutils.unittest;



import org.junit.runner.RunWith;
import org.junit.runners.Suite;



@RunWith(Suite.class)
@Suite.SuiteClasses({
  StreamManagerTest.class,
  ThreadContextConverterTest.class,
  ValidatorTest.class
})

public class RunAllTests {
  // the class remains empty,
  // used only as a holder for the above annotations
}
