/*
 * Copyright (C) 2008 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.inject.testing.guiceberry.junit3;

import com.google.inject.AbstractModule;
import com.google.inject.Binder;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import com.google.inject.testing.guiceberry.GuiceBerryEnv;
import com.google.inject.testing.guiceberry.NoOpTestScopeListener;
import com.google.inject.testing.guiceberry.TestId;
import com.google.inject.testing.guiceberry.TestScopeListener;
import com.google.common.base.Objects;
import com.google.common.testing.TearDown;
import com.google.common.testing.junit3.JUnitAsserts;
import com.google.common.testing.junit3.TearDownTestCase;

import junit.framework.TestCase;

/**
 * Tests the {@link GuiceBerryJunit} class.
 * 
 * @author Luiz-Otavio Zorzella
 * @author Danka Karwanska
 */
public class GuiceBerryJunitTest extends TearDownTestCase {
  
  /*
   * There's unfortunatelly no way to statically get a class' canonical way
   * through reflection.
   */
  private static final String SELF_CANONICAL_NAME = 
    "com.google.inject.testing.guiceberry.junit3.GuiceBerryJunitTest";
  
  private static final String GUICE_BERRY_ENV_THAT_DOES_NOT_EXIST = 
    "com.this.guice.berry.env.does.NotExist";
  
  private static final String NOT_A_GUICE_BERRY_ENV_BECAUSE_IT_IS_ABSTRACT = 
    "com.google.inject.AbstractModule";
    
  private static final String INJECTED_INFORMATION = "Injected information";  

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    
    TearDown tearDown = new TearDown() {
      @Override
      public void tearDown() throws Exception {    
        GuiceBerryJunit.clear();
      }
    };
    addRequiredTearDown(tearDown);
  }

  public void testSelfCanonicalNameConstantIsCorrect() throws Exception {
    
    String message = 
      "The constant SELF_CANONICAL_NAME does not match this class's \n" +
      "canonical name (e.g. this class has just been moved or renamed).\n" +
      "\n" +
      "There's unfortunatelly no way to statically get a class' canonical \n" +
      "name through reflection, and, thus, this constant has to be manually\n" +
      "updated. \n" +
      "\n" +
      "Several tests will fail after this, until this is fixed.";
    assertEquals(message, 
        this.getClass().getCanonicalName(), SELF_CANONICAL_NAME);
    
  }
  
  public void testWithNoAnnotationThrowsException() {
    try {
      GuiceBerryJunit.setUp(this);
      fail();
    } catch (NullPointerException expected) {
    }
  }
  
  public void testAnnotationWithModuleThatNotExistsThrowsException() {
   try {
      TestAnnotatedWithModuleThatNotExist testClass = 
        TestAnnotatedWithModuleThatNotExist.createInstance();
      GuiceBerryJunit.setUp(testClass);
      fail();
   } catch (IllegalArgumentException expected) { }
  }
 
  public void testAnnotationWithModuleThatHasMissingBindingsThrowsException() {   
    try {
      TestAnnotatedWithModuleThatHasMissingBindings testClass = 
        TestAnnotatedWithModuleThatHasMissingBindings.createInstance();
      GuiceBerryJunit.setUp(testClass);
      fail();
    } catch (RuntimeException expected) {
      //TODO: we should assert expected's cause is ConfigurationException, but 
      //that exception is private
      assertTrue(expected.getMessage().startsWith("Binding error in the module"));
      String configurationExceptionSuffix = "Binding to " +
        BarService.class.getName() +
        " not found. No bindings to that type were found.";
      assertTrue(expected.getCause().getMessage().endsWith(configurationExceptionSuffix));
    }
  }
 
  public void testAnnotationWithClassThatNotImplementsModuleThrowsException() {
    try {
      TestAnnotatedWithClassThatNotImplementsModule testClass = 
        TestAnnotatedWithClassThatNotImplementsModule.createInstance();
      GuiceBerryJunit.setUp(testClass);
      fail();
    } catch (IllegalArgumentException expected) { }
  }
     
  public void testAnnotationWithClassThatHasWrongConstructorThrowsException() {
    try {
      TestAnnotatedWithModuleThatHAsWrongConstructor testClass = 
        TestAnnotatedWithModuleThatHAsWrongConstructor.createInstance();
      GuiceBerryJunit.setUp(testClass);
      fail();
    } catch (RuntimeException expected) {
      assertTrue(expected.getCause() instanceof NoSuchMethodException);
    }
  }
 
  public void testAnotatedClassWithAbstractClassModuleThrowsException() {
    TestAnnotatedWithModuleThatIsAbstractClass testClass = 
      TestAnnotatedWithModuleThatIsAbstractClass.createInstance();
    try {
      GuiceBerryJunit.setUp(testClass);
      fail();
    } catch (RuntimeException expected) {
      assertTrue(expected.getCause() instanceof InstantiationException);
    }
  }
  
  public void testAnnotaionWithAbstractModuleThatImplementsOtherInterfaces() {      
    TestAnnotatedWithStubService1 testClass =
      TestAnnotatedWithStubService1.createInstance();    
    GuiceBerryJunit.setUp(testClass);    
  }
  
  public void testThatTestCaseGetsInjectedWithSomething() {
    TestAnnotatedWithStubService1 testClass = 
      TestAnnotatedWithStubService1.createInstance();
    GuiceBerryJunit.setUp(testClass);
    assertNotNull(testClass.barService);
    assertNotNull(testClass.fooService);
  }

  private static class MyThread extends Thread {
    private TestCase theTestCase;

    @Override
    public void run() {
      theTestCase = GuiceBerryJunit.getActualTestCase();
    }
  }
  
  public void testThatThreadLocalsInherit() throws InterruptedException {
    TestAnnotatedWithStubService1 testClass = 
      TestAnnotatedWithStubService1.createInstance();
    GuiceBerryJunit.setUp(testClass);

    // Finding the current test case from a secondary thread.
    MyThread myThread = new MyThread();
    myThread.start();   
    myThread.join();
    
    assertNotNull(myThread.theTestCase);
    assertEquals(TestAnnotatedWithStubService1.class.getName(), 
        myThread.theTestCase.getClass().getName());

  }

  public void testThatTestCaseGetsInjectedWithWhatsConfiguredInTheModule() {
   TestAnnotatedWithStubService1 testClass = 
     TestAnnotatedWithStubService1.createInstance();
   GuiceBerryJunit.setUp(testClass);
   assertTrue(testClass.barService instanceof BarServiceOne);
   assertTrue(testClass.fooService instanceof FooServiceOne);
  }
  
  public void testInjectorMapIsSetAfterATest() throws ClassNotFoundException {
    TestAnnotatedWithStubService1 firstTest = 
      TestAnnotatedWithStubService1.createInstance();
    Injector injector = GuiceBerryJunit.getInjectorFromGB(
          Class.forName(GuiceBerryEnvOne.GUICE_BERRY_ENV_ONE));
    assertNull(injector);
    
    GuiceBerryJunit.setUp(firstTest);
    injector = GuiceBerryJunit.getInjectorFromGB(
        Class.forName(GuiceBerryEnvOne.GUICE_BERRY_ENV_ONE));
    
    assertNotNull(injector);
  }
  
  public void testNumberOfInjectorsNotChangesForTestCaseThatDeclaresSameModule() 
      throws ClassNotFoundException {
    TestAnnotatedWithStubService1 firstTest = 
      TestAnnotatedWithStubService1.createInstance();
    GuiceBerryJunit.setUp(firstTest);
    
    Injector firstInjector = 
      GuiceBerryJunit.getInjectorFromGB(
          Class.forName(GuiceBerryEnvOne.GUICE_BERRY_ENV_ONE));
    GuiceBerryJunit.tearDown(firstTest);

    TestAnnotatedWithStubService2 secondTest = 
      TestAnnotatedWithStubService2.createInstance();
    GuiceBerryJunit.setUp(secondTest);
    
    Injector secondInjector = 
      GuiceBerryJunit.getInjectorFromGB(
          Class.forName(GuiceBerryEnvOne.GUICE_BERRY_ENV_ONE));
    
    assertSame(firstInjector, secondInjector);    
    assertEquals(1, GuiceBerryJunit.numberOfInjectorsInUse());    
  }
  
  public void testReUseingInjectorForTestCasesThatDeclaresSameModuleName() {
    
    TestAnnotatedWithStubService1 firstTest = 
      TestAnnotatedWithStubService1.createInstance();
    GuiceBerryJunit.setUp(firstTest);    
    GuiceBerryJunit.tearDown(firstTest);
    
    TestAnnotatedWithStubService2 secondTest = 
      TestAnnotatedWithStubService2.createInstance();
    GuiceBerryJunit.setUp(secondTest);
    assertEquals(firstTest.number, secondTest.number);

  }

  public void testNotReUseingInjectorForTestsThatDeclaresDifferentModules() {
    
    TestAnnotatedWithStubService1 firstTest = 
      TestAnnotatedWithStubService1.createInstance();
    GuiceBerryJunit.setUp(firstTest);    
    GuiceBerryJunit.tearDown(firstTest);
    
    TestAnnotatedWithRealService secondTest = 
      TestAnnotatedWithRealService.createInstance();
    GuiceBerryJunit.setUp(secondTest);
    JUnitAsserts.assertNotEqual(firstTest.number, secondTest.number);

  }

  public void testPutTwoInjectorsInMapForTestsThatDeclareDifferentModules() {
   
    TestAnnotatedWithStubService1 firstTest = 
      TestAnnotatedWithStubService1.createInstance();
    
    GuiceBerryJunit.setUp(firstTest);
    GuiceBerryJunit.tearDown(firstTest);
    
    TestAnnotatedWithRealService secondTest = 
      TestAnnotatedWithRealService.createInstance();
    
    GuiceBerryJunit.setUp(secondTest);
   
    assertEquals(2, GuiceBerryJunit.numberOfInjectorsInUse());    
   }
  
  public void testCreateingCascadingInjections() {
    
    TestAnnotatedWithRealService testClass = 
      TestAnnotatedWithRealService.createInstance();
    GuiceBerryJunit.setUp(testClass);       
    assertEquals(INJECTED_INFORMATION, testClass.fooService.get());
   }
  
  public void testSystemPropertyOverridesModule() {
    TestAnnotatedWithStubService1 testClass = 
      TestAnnotatedWithStubService1.createInstance();
    
    TearDown tearDown = new TearDown() {
    
      @Override
      public void tearDown() throws Exception {
        System.clearProperty(GuiceBerryJunit
            .buildModuleOverrideProperty(GuiceBerryEnvOne.GUICE_BERRY_ENV_ONE));
      }
    };
    addRequiredTearDown(tearDown);
    System.setProperty(GuiceBerryJunit
        .buildModuleOverrideProperty(GuiceBerryEnvOne.GUICE_BERRY_ENV_ONE), 
        GuiceBerryEnvTwo.GUICE_BERRY_ENV_TWO);
    
    GuiceBerryJunit.setUp(testClass);
    assertTrue(testClass.barService instanceof BarServiceTwo);
    assertTrue(testClass.fooService instanceof FooServiceTwo);
  }
  
  public void testNotExistingModuldeOverridesModuleThrowsException() {
    TestAnnotatedWithStubService1 testClass = 
      TestAnnotatedWithStubService1.createInstance();
    
    TearDown tearDown = new TearDown() {
    
      @Override
      public void tearDown() throws Exception {
        System.clearProperty(GuiceBerryJunit
            .buildModuleOverrideProperty(GuiceBerryEnvOne.GUICE_BERRY_ENV_ONE));
      }
    };
    
    addRequiredTearDown(tearDown);
    
    System.setProperty(GuiceBerryJunit
        .buildModuleOverrideProperty(
         GuiceBerryEnvOne.GUICE_BERRY_ENV_ONE), GUICE_BERRY_ENV_THAT_DOES_NOT_EXIST);
    
    try {
      GuiceBerryJunit.setUp(testClass);
      fail();
    } catch (IllegalArgumentException expected) {}
  }

  public void testTestGetsInjectedWithTestId() {
    
    TestAnnotatedWithStubService1 testClass =  
      TestAnnotatedWithStubService1.createInstance();
    
    assertEquals(null, testClass.testId);
    GuiceBerryJunit.setUp(testClass);
    assertEquals(
        new TestId(testClass.getClass().getName(), testClass.getName()),
        testClass.testId);
  } 
  
  public void testDifferentTestsGetInjectedWithDifferentTestId() {
    
    TestAnnotatedWithStubService1 firstTest =  
      TestAnnotatedWithStubService1.createInstance();
    
    GuiceBerryJunit.setUp(firstTest);
    
    assertEquals(
        new TestId(firstTest.getClass().getName(), firstTest.getName()),
        firstTest.testId);
    GuiceBerryJunit.tearDown(firstTest);
   
    TestAnnotatedWithStubService2 secondTest = 
      TestAnnotatedWithStubService2.createInstance();
    GuiceBerryJunit.setUp(secondTest);
   
    assertEquals(
        new TestId(secondTest.getClass().getName(), secondTest.getName()),
        secondTest.testId);
  }
  
  public void testTestIdGetsInjectedIntoRealServiceDefinedByModule() {
    
    TestAnnotatedWithRealService testClass = 
      TestAnnotatedWithRealService.createInstance();
    
    assertEquals(null, testClass.barService);    
    
    GuiceBerryJunit.setUp(testClass);
    
    assertNotNull(testClass.barService.getTestId());
    assertEquals(
        new TestId(testClass.getClass().getName(), testClass.getName()),
        testClass.barService.getTestId());
  }
  
  public void testTestGetsInjectedWithTestCase() {  
    TestAnnotatedWithStubService1 testClass =  
      TestAnnotatedWithStubService1.createInstance();
    
    assertEquals(null, testClass.testCase);
    GuiceBerryJunit.setUp(testClass);
    assertEquals(testClass.getName(), testClass.testCase.getName());
  } 
  
  
  public void testDifferentTestsGetsInjectedWithDifferentTestCases() {
    
    TestAnnotatedWithStubService1 firstTest =  
      TestAnnotatedWithStubService1.createInstance();
    
    GuiceBerryJunit.setUp(firstTest);
    
    assertEquals(firstTest.getName(), firstTest.testCase.getName());
    
    GuiceBerryJunit.tearDown(firstTest);
   
    TestAnnotatedWithStubService2 secondTest = 
      TestAnnotatedWithStubService2.createInstance();
    GuiceBerryJunit.setUp(secondTest);
    
    assertEquals(secondTest.getName(), secondTest.testCase.getName());
    
    JUnitAsserts.assertNotEqual(firstTest.testCase, secondTest.testCase);
  }

  public void testMethodTearDownWorksProperly() {
    TestAnnotatedWithStubService1 testClass = 
      TestAnnotatedWithStubService1.createInstance();
    
    GuiceBerryJunit.setUp(testClass);
    assertEquals(testClass,  GuiceBerryJunit.getActualTestCase());
    GuiceBerryJunit.tearDown(testClass); 
  //No concurrence problems as the actual TestCase is: ThreadLocal<TestCase>
    assertNull(GuiceBerryJunit.getActualTestCase());
  }
  
  public void testMethodTearDownNoPreviousSetupOnClassWithNoAnnotation() {
    try {
      GuiceBerryJunit.tearDown(this);
      fail();
    } catch (NullPointerException expected) {}
  }
  
  
  public void testMethodTearDownNoPreviousSetupOnClassWithAnnotation() {
    
    TestAnnotatedWithStubService1 testClass = 
      TestAnnotatedWithStubService1.createInstance();
    try {
      GuiceBerryJunit.tearDown(testClass);
      fail();
    } catch (RuntimeException expected) {}  
  }
  
  
  public void testTearDownNoPreviousSetupOnClassWithAnnotationThatWasUsed() {

    TestAnnotatedWithStubService1 testClass1 = 
      TestAnnotatedWithStubService1.createInstance();
    GuiceBerryJunit.setUp(testClass1);
    testClass1.run();
    TestAnnotatedWithStubService1 testClass2 = 
      TestAnnotatedWithStubService1.createInstance();
    try {
      GuiceBerryJunit.tearDown(testClass2);
      fail();
    } catch (RuntimeException expected) {}
  }
  
  
  public void testTearDownOnDifferentClassThatSetupWasCalled() {

    TestAnnotatedWithStubService1 testClass1 = 
      TestAnnotatedWithStubService1.createInstance();
    GuiceBerryJunit.setUp(testClass1);
    TestAnnotatedWithStubService2 testClass2 = 
      TestAnnotatedWithStubService2.createInstance();
    try {
      GuiceBerryJunit.tearDown(testClass2);
      fail();
    } catch (RuntimeException expected) {}
  }
  
 
  public void testCallingTwoSetupWithNoTearDownBetween() {
    TestAnnotatedWithStubService1 testClass = 
      TestAnnotatedWithStubService1.createInstance();
    try {
      GuiceBerryJunit.setUp(testClass);
      GuiceBerryJunit.setUp(testClass);
      fail();
    } catch (RuntimeException expected) {}   
  }
  
  public void testAddRequiredTearDownToTearDownTestCase() {
    TestAnnotatedWithStubService1 testClass = 
      TestAnnotatedWithStubService1.createInstance();
   
    GuiceBerryJunit.setUp(testClass);
    assertEquals(testClass, testClass.testCase);
    testClass.run();
  //No concurrence problems as the actual TestCase is: ThreadLocal<TestCase>
    assertNull(GuiceBerryJunit.getActualTestCase());
  }

  public void testTestCaseCanBeUsedInsteadOfTearDownTestCase() {
    TestCaseAnnotatedWithStubService testClass = 
      TestCaseAnnotatedWithStubService.createInstance();
    GuiceBerryJunit.setUp(testClass);
    testClass.run();     
  }
  
  public void testMethodTearDownForTestCaseNotCalledAutomatically() {
    TestCaseAnnotatedWithStubService testClass = 
      TestCaseAnnotatedWithStubService.createInstance();
    GuiceBerryJunit.setUp(testClass);
    testClass.run();

    TestAnnotatedWithStubService1 testClass2 = 
      TestAnnotatedWithStubService1.createInstance();
    try {  
      GuiceBerryJunit.setUp(testClass2);
      fail();
    } catch (RuntimeException expected) {}
  }
 
  public void testMethodTearDownForTestCaseCalledManually() {
    TestCaseAnnotatedWithStubService testClass = 
      TestCaseAnnotatedWithStubService.createInstance();
    GuiceBerryJunit.setUp(testClass);   
    testClass.run();
 
    GuiceBerryJunit.tearDown(testClass);
    TestAnnotatedWithStubService1 testClass2 = 
      TestAnnotatedWithStubService1.createInstance();
    GuiceBerryJunit.setUp(testClass2);
    testClass2.run();
  }   
 
  public void testAnnotationWithModuleThaHasNoTestScopeListenerBinding() {
    TestAnnotatedWithModuleThatProvidedNoTestScopeListener testClass = 
      TestAnnotatedWithModuleThatProvidedNoTestScopeListener.createInstance();
    try {
      GuiceBerryJunit.setUp(testClass);
      fail();
    } catch (RuntimeException expected) {}  
  }
  
  public void testModuleThatBindsTestScopeListenerToNoOpTestScopeListener() 
      throws ClassNotFoundException {
    TestAnnotatedWithStubService1 testClass = 
      TestAnnotatedWithStubService1.createInstance();

    GuiceBerryJunit.setUp(testClass);
    TestScopeListener scopeListener =
      GuiceBerryJunit.getInjectorFromGB(
      Class.forName(GuiceBerryEnvOne.GUICE_BERRY_ENV_ONE))
      .getInstance(TestScopeListener.class);
    
    assertTrue(scopeListener instanceof NoOpTestScopeListener);    
  }
  
  public void testModuleThatBindsTestScopeListenerToSomeScopeListener() 
      throws ClassNotFoundException {
    TestAnnotatedWithRealService testClass1 = 
      TestAnnotatedWithRealService.createInstance();
      
      GuiceBerryJunit.setUp(testClass1);
      TestScopeListener scopeListener =
        GuiceBerryJunit.getInjectorFromGB(
        Class.forName(GuiceBerryEnvTwo.GUICE_BERRY_ENV_TWO))
        .getInstance(TestScopeListener.class);
      
      assertTrue(scopeListener instanceof BazService);    
  }
 
  public void testTearDownOnModuleNoTestScopeListenerBindingNoPreviousSetUp() {

    TestAnnotatedWithModuleThatProvidedNoTestScopeListener testClass = 
      TestAnnotatedWithModuleThatProvidedNoTestScopeListener.createInstance();
    
    try {
      GuiceBerryJunit.tearDown(testClass);
      fail();
    } catch (RuntimeException expected) {}  
  }
   
  public void testTestScopeListenerGetsNotifiesThatTestEntersTheScope() {
    TestAnnotatedWithRealService testClass = 
      TestAnnotatedWithRealService.createInstance();
   
    long baz = BazService.counter;
    
    assertNotNull(baz);
    GuiceBerryJunit.setUp(testClass);
    assertNotNull(testClass.baz);
    long baz2 = testClass.baz.getCounter();
     
    assertTrue(baz < baz2);   
    
  }
 
  public void testTestScopeListenerGetsNotifiesThatTestExitsTheScope() {
    TestAnnotatedWithRealService testClass = 
      TestAnnotatedWithRealService.createInstance();
    
    GuiceBerryJunit.setUp(testClass);
    assertNotNull(testClass.baz);
    long baz = testClass.baz.getCounter();
    GuiceBerryJunit.tearDown(testClass);
    long baz2 = testClass.baz.getCounter();
    assertTrue(baz < baz2);   
  }

  public void testTestScopeIsCreatedForModule() 
      throws ClassNotFoundException {
    TestAnnotatedWithStubService1 testClass = 
      TestAnnotatedWithStubService1.createInstance();
    
   assertNull(GuiceBerryJunit.getTestScopeFromGB(
       Class.forName(GuiceBerryEnvOne.GUICE_BERRY_ENV_ONE)));
    GuiceBerryJunit.setUp(testClass);
    JunitTestScope testScope = 
      GuiceBerryJunit.getTestScopeFromGB(
          Class.forName(GuiceBerryEnvOne.GUICE_BERRY_ENV_ONE));
    assertNotNull(testScope);
  }  
 
  public void testReUseTestScopeByTwoTestsAnnotatedWithTheSameModule() 
    throws ClassNotFoundException{
    TestAnnotatedWithStubService1 testClass = 
      TestAnnotatedWithStubService1.createInstance();
    assertNull(
        GuiceBerryJunit.getTestScopeFromGB(
            Class.forName(GuiceBerryEnvOne.GUICE_BERRY_ENV_ONE)));
    GuiceBerryJunit.setUp(testClass);
    JunitTestScope testScope = 
      GuiceBerryJunit.getTestScopeFromGB(
          Class.forName(GuiceBerryEnvOne.GUICE_BERRY_ENV_ONE));
    assertNotNull(testScope);
    
    testClass.run();
   
    TestAnnotatedWithStubService2 testClass2 = 
      TestAnnotatedWithStubService2.createInstance();
    GuiceBerryJunit.setUp(testClass2);
    assertNotNull(GuiceBerryJunit.getTestScopeFromGB(
        Class.forName(GuiceBerryEnvOne.GUICE_BERRY_ENV_ONE)));
    
    JunitTestScope testScope2 = 
      GuiceBerryJunit.getTestScopeFromGB(
          Class.forName(GuiceBerryEnvOne.GUICE_BERRY_ENV_ONE));
    assertSame(testScope, testScope2);
    assertEquals(1, GuiceBerryJunit.numberOfInjectorsInUse());
  }  

  public void testUseDifferentTestScopeByTwoTestsAnnotatedWithDifferentModule() 
      throws ClassNotFoundException {
    TestAnnotatedWithStubService1 testClass = 
      TestAnnotatedWithStubService1.createInstance();
   
    assertNull(GuiceBerryJunit.getTestScopeFromGB(
        Class.forName(GuiceBerryEnvOne.GUICE_BERRY_ENV_ONE)));
    GuiceBerryJunit.setUp(testClass);
    JunitTestScope testScope = 
      GuiceBerryJunit.getTestScopeFromGB(
          Class.forName(GuiceBerryEnvOne.GUICE_BERRY_ENV_ONE));
    assertNotNull(testScope);
  
    testClass.run();
    
    TestAnnotatedWithRealService testClass2 = 
      TestAnnotatedWithRealService.createInstance();
    GuiceBerryJunit.setUp(testClass2);
    assertNotNull(GuiceBerryJunit.getTestScopeFromGB(
        Class.forName(GuiceBerryEnvTwo.GUICE_BERRY_ENV_TWO)));
    JunitTestScope testScope2 = 
      GuiceBerryJunit.getTestScopeFromGB(
          Class.forName(GuiceBerryEnvTwo.GUICE_BERRY_ENV_TWO));
    
    assertNotSame(testScope, testScope2); 
  }
  
  public void testInjectingTestCasesIntoTestScopeListeners() 
      throws Exception {
    TestAnnotatedWitthModuleInjectsTestCaseInTestScopeListener testClass = 
      TestAnnotatedWitthModuleInjectsTestCaseInTestScopeListener.createInstance();
    
    GuiceBerryJunit.setUp(testClass);
    testClass.run();
  }
  
//THE BELOW CLASSES ARE USED ONLY FOR TESTING GuiceBerry
  
  @GuiceBerryEnv(GuiceBerryEnvOne.GUICE_BERRY_ENV_ONE)
  private static final class TestAnnotatedWithStubService1 
      extends TearDownTestCase {
    @Inject
    private BarService barService; 
    
    @Inject
    private FooService fooService;
    
    @Inject
    private int number;
    
    @Inject
    private TestId testId;

    @Inject
    private TestCase testCase; 
  
    static TestAnnotatedWithStubService1 createInstance() {
      TestAnnotatedWithStubService1 result = 
        new TestAnnotatedWithStubService1();
      result.setName(TestAnnotatedWithStubService1
          .class.getCanonicalName());
      return result;
    }    
  }
  
  @GuiceBerryEnv(GuiceBerryEnvOne.GUICE_BERRY_ENV_ONE)
  private static final class TestAnnotatedWithStubService2 
      extends TearDownTestCase {
    
    @Inject
    private int number;
    
    @Inject
    private TestId testId;
    
    @Inject 
    private TestCase testCase;

    static TestAnnotatedWithStubService2 createInstance() {
      TestAnnotatedWithStubService2 result = 
        new TestAnnotatedWithStubService2();
      result.setName(TestAnnotatedWithStubService2.class.getCanonicalName()); 
      return result;
    }
  }
  
  @GuiceBerryEnv(GuiceBerryEnvOne.GUICE_BERRY_ENV_ONE)
  private static final class TestCaseAnnotatedWithStubService extends TestCase {
    @Inject
    TestCase testCase;
    
    static TestCaseAnnotatedWithStubService createInstance() {
      TestCaseAnnotatedWithStubService result = 
        new TestCaseAnnotatedWithStubService();
      result.setName(TestCaseAnnotatedWithStubService
          .class.getCanonicalName());
      return result;
    }  
  }
  
  @GuiceBerryEnv(GuiceBerryEnvTwo.GUICE_BERRY_ENV_TWO)
  private static final class TestAnnotatedWithRealService 
      extends TearDownTestCase {
    @Inject
    private FooService fooService;

    @Inject
    private BarService barService;  
    
    @Inject
    private int number;
 
    @Inject
    private BazService baz;
   
    static TestAnnotatedWithRealService createInstance() {
      TestAnnotatedWithRealService result = new TestAnnotatedWithRealService();
      result.setName(TestAnnotatedWithRealService.class.getCanonicalName());
      return result;
    }  
  }
  
  @GuiceBerryEnv(GUICE_BERRY_ENV_THAT_DOES_NOT_EXIST)
  private static final class TestAnnotatedWithModuleThatNotExist 
      extends TearDownTestCase {
    static TestAnnotatedWithModuleThatNotExist createInstance() {
      TestAnnotatedWithModuleThatNotExist result = 
        new TestAnnotatedWithModuleThatNotExist();
      result.setName(TestAnnotatedWithModuleThatNotExist
          .class.getCanonicalName());
      return result;
    }  
  }

  @GuiceBerryEnv(GuiceBerryEnvWithoutBindingsForFooOrBar.GUICE_BERRY_ENV_WITHOUT_BINDINGS_FOR_FOO_OR_BAR)
  private static final class TestAnnotatedWithModuleThatHasMissingBindings 
      extends TearDownTestCase {
    @Inject
    BarService barService; 
    
    static TestAnnotatedWithModuleThatHasMissingBindings createInstance() {
      TestAnnotatedWithModuleThatHasMissingBindings result = 
        new TestAnnotatedWithModuleThatHasMissingBindings();
      result.setName(TestAnnotatedWithModuleThatHasMissingBindings
          .class.getCanonicalName());
      return result;
    }  
  }
  
  @GuiceBerryEnv(NotAGuiceBerryEnvOne.NOT_A_GUICE_BERRY_ENV_ONE)
  private static final class TestAnnotatedWithClassThatNotImplementsModule 
      extends TearDownTestCase {
    
    static TestAnnotatedWithClassThatNotImplementsModule createInstance() {
      TestAnnotatedWithClassThatNotImplementsModule result = 
        new TestAnnotatedWithClassThatNotImplementsModule();
      result.setName(TestAnnotatedWithClassThatNotImplementsModule
          .class.getCanonicalName());
      return result;
    }  
  }

  @GuiceBerryEnv(GuiceBerryEnvWithIllegalConstructor.GUICE_BERRY_ENV_WITH_ILLEGAL_CONSTRUCTOR)
  private static final class TestAnnotatedWithModuleThatHAsWrongConstructor
    extends TearDownTestCase {
    
    static TestAnnotatedWithModuleThatHAsWrongConstructor createInstance() {
      TestAnnotatedWithModuleThatHAsWrongConstructor result = 
        new TestAnnotatedWithModuleThatHAsWrongConstructor();
      result.setName(TestAnnotatedWithModuleThatHAsWrongConstructor
          .class.getCanonicalName());
      return result;
    }  
  }

  @GuiceBerryEnv(GuiceBerryEnvWithNoTestScopeListener.GUICE_BERRY_ENV_WITH_NO_TEST_SCOPE_LISTENER)
  private static final class TestAnnotatedWithModuleThatProvidedNoTestScopeListener 
      extends TearDownTestCase {
    
    static TestAnnotatedWithModuleThatProvidedNoTestScopeListener createInstance() {
      TestAnnotatedWithModuleThatProvidedNoTestScopeListener result = 
        new TestAnnotatedWithModuleThatProvidedNoTestScopeListener();
      result.setName(TestAnnotatedWithModuleThatProvidedNoTestScopeListener
          .class.getCanonicalName());
      return result;
    }  
  }

  @GuiceBerryEnv(NOT_A_GUICE_BERRY_ENV_BECAUSE_IT_IS_ABSTRACT)
  private static final class TestAnnotatedWithModuleThatIsAbstractClass 
      extends TearDownTestCase {
    
    static TestAnnotatedWithModuleThatIsAbstractClass createInstance() {
      TestAnnotatedWithModuleThatIsAbstractClass result = 
        new TestAnnotatedWithModuleThatIsAbstractClass();
      result.setName(TestAnnotatedWithModuleThatIsAbstractClass
          .class.getCanonicalName());
      return result;
    }  
  }
  
  @GuiceBerryEnv(GuiceBerryEnvWithNonTrivialTestScopeListener.MODULE_NAME_INJECTS_TEST_CASE_IN_TEST_SCOPE_LISTENER)
  private static final class TestAnnotatedWitthModuleInjectsTestCaseInTestScopeListener
      extends TestCase {
    
    static TestAnnotatedWitthModuleInjectsTestCaseInTestScopeListener createInstance() {
      TestAnnotatedWitthModuleInjectsTestCaseInTestScopeListener result = 
        new TestAnnotatedWitthModuleInjectsTestCaseInTestScopeListener();
      result.setName(TestAnnotatedWitthModuleInjectsTestCaseInTestScopeListener.class
          .getCanonicalName());
      return result;
    }  
  }
  
// BELOW CLASSES IMPLEMENTS INTERFACE MODULE
// USED FOR GuiceBerryEnv ANNOTATIONS -- only for testing  
  
  private static int NUMBER = 0;
  
  public static class GuiceBerryEnvOne extends AbstractModule {
    private static final String GUICE_BERRY_ENV_ONE = 
    GuiceBerryJunitTest.SELF_CANONICAL_NAME + "$GuiceBerryEnvOne";

    @Override
    public void configure() {
      install(new BasicModule());
      bind(BarService.class).to(BarServiceOne.class);
      bind(FooService.class).to(FooServiceOne.class);
      bind(Integer.class).toInstance(NUMBER++);
      bind(TestScopeListener.class).toInstance(new NoOpTestScopeListener());
    }
  }

  public static class GuiceBerryEnvTwo extends AbstractModule {
    private static final String GUICE_BERRY_ENV_TWO = 
    GuiceBerryJunitTest.SELF_CANONICAL_NAME + "$GuiceBerryEnvTwo";

    @Override
    public void configure() {
      install(new BasicModule());
      bind(FooService.class).to(FooServiceTwo.class);
      bind(BarService.class).to(BarServiceTwo.class);      
      bind(BazService.class).in(Singleton.class);
      bind(Integer.class).toInstance(NUMBER++);
      bind(String.class).toInstance(INJECTED_INFORMATION);
      bind(TestScopeListener.class).to(BazService.class).in(Scopes.SINGLETON);
    }
  }

  public static class GuiceBerryEnvWithoutBindingsForFooOrBar 
      extends AbstractModule  {
    private static final String GUICE_BERRY_ENV_WITHOUT_BINDINGS_FOR_FOO_OR_BAR =
    GuiceBerryJunitTest.SELF_CANONICAL_NAME + "$GuiceBerryEnvWithoutBindingsForFooOrBar";

    @Override
    public void configure() {
      install(new BasicModule());
      bind(TestScopeListener.class).toInstance(new NoOpTestScopeListener());
    }
  }

  public static class GuiceBerryEnvWithNonTrivialTestScopeListener 
      extends AbstractModule {
    private static final String MODULE_NAME_INJECTS_TEST_CASE_IN_TEST_SCOPE_LISTENER = 
    GuiceBerryJunitTest.SELF_CANONICAL_NAME + "$GuiceBerryEnvWithNonTrivialTestScopeListener";

    @Override
    public void configure() {
      install(new BasicModule());
      bind(TestScopeListener.class)
        .toInstance(new TestScopeListenerGetsInjectedWithTestCase());     
    }
  }
  
  
  public static class GuiceBerryEnvWithIllegalConstructor implements Module {    
    private static final String GUICE_BERRY_ENV_WITH_ILLEGAL_CONSTRUCTOR = 
    GuiceBerryJunitTest.SELF_CANONICAL_NAME + "$GuiceBerryEnvWithIllegalConstructor";

    /**
     * Constructors should be no-args
     */
    public GuiceBerryEnvWithIllegalConstructor(int a){}
    
    @Override
    public void configure(Binder binder) {}
  }

  public static class GuiceBerryEnvWithNoTestScopeListener 
      extends AbstractModule {    
    private static final String GUICE_BERRY_ENV_WITH_NO_TEST_SCOPE_LISTENER = 
      GuiceBerryJunitTest.SELF_CANONICAL_NAME + "$GuiceBerryEnvWithNoTestScopeListener";

    @Override
    public void configure() {
      install(new BasicModule());
      bind(FooService.class).to(FooServiceTwo.class);
      bind(BarService.class).to(BarServiceTwo.class);      
      bind(Integer.class).toInstance(NUMBER++);
      bind(String.class).toInstance(INJECTED_INFORMATION);
    }
  }   
  
  /**
   * {@link GuiceBerryEnv}s must be {@link Module}s.
   */
  public static class NotAGuiceBerryEnvOne {

    private static final String NOT_A_GUICE_BERRY_ENV_ONE = 
    GuiceBerryJunitTest.SELF_CANONICAL_NAME + "$NotAGuiceBerryEnvOne"; }

//BELOW CLASSES ARE USED TO TEST IF GUICEBERRY BINDS THINGS PROPERLY   
// used only for testing
 
  private static class BazService implements TestScopeListener {
    private static long counter = 0;
    
    public void enteringScope() {
      counter++; 
    }

    public void exitingScope() {
      counter++; 
    }
    
    long getCounter(){
      return counter;
    }  
  }  
  
  private  interface FooService {
    public String get();
  }
    
  private static class TestScopeListenerGetsInjectedWithTestCase implements TestScopeListener {
    @Inject
    TestCase testCase;
    
    public void enteringScope() {
      Objects.nonNull(testCase, "TestCase is null, ");
    }

    public void exitingScope() { }
  }
  
  private static class FooServiceTwo implements FooService {
    
    @Inject private String information;
    
    public String get(){
      return information;
    }
  }
     
  private static class FooServiceOne implements FooService { 
    @Inject private String information;
    
    public String get(){
      return information;
    }
  }
     
  private interface BarService {
    public TestId getTestId();
  }
     
  private static class BarServiceTwo implements BarService {
    @Inject 
    private TestId testId; 
    
    public TestId getTestId(){
      return testId;
    }
  }
         
  private static class BarServiceOne implements BarService {
    
    public TestId getTestId(){
      return null;
    }
  } 
}