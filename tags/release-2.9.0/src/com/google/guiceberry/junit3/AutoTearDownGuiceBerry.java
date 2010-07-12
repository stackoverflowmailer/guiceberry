/*
 * Copyright (C) 2010 Google Inc.
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
package com.google.guiceberry.junit3;

import com.google.common.testing.TearDown;
import com.google.common.testing.junit3.TearDownTestCase;
import com.google.guiceberry.DefaultEnvSelector;
import com.google.guiceberry.GuiceBerryEnvSelector;
import com.google.guiceberry.GuiceBerry;
import com.google.guiceberry.GuiceBerry.GuiceBerryWrapper;
import com.google.inject.Module;

/**
 * {@link GuiceBerry} adapter for JUnit3 {@link TearDownTestCase}s.
 *
 * @see ManualTearDownGuiceBerry
 *
 * @author Luiz-Otavio "Z" Zorzella
 */
public class AutoTearDownGuiceBerry {

  /**
   * Sets up the test, by creating an injector for the given
   * {@code guiceBerryEnvClass} and registering a {@link TearDown} against the
   * given {@code testCase}.
   */
  public static void setUp(TearDownTestCase testCase, Class<? extends Module> guiceBerryEnvClass) {
    setUp(testCase, DefaultEnvSelector.of(guiceBerryEnvClass));
  }
  
  /**
   * Same as {@link #setUp(TearDownTestCase, Class)}, but with the given 
   * {@code guiceBerryEnvSelector}.
   */
  public static void setUp(TearDownTestCase testCase, GuiceBerryEnvSelector guiceBerryEnvSelector) {
    final GuiceBerryWrapper toTearDown = 
      GuiceBerry.INSTANCE.buildWrapper(
          ManualTearDownGuiceBerry.buildTestDescription(testCase, testCase.getName()), guiceBerryEnvSelector);
    testCase.addTearDown(new TearDown() {
      
      public void tearDown() throws Exception {
        toTearDown.runAfterTest();
      }
    })  ;
    toTearDown.runBeforeTest();
  }
}