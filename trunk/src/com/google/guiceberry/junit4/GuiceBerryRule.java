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
package com.google.guiceberry.junit4;

import com.google.guiceberry.GuiceBerry;
import com.google.guiceberry.TestDescription;
import com.google.guiceberry.TestId;
import com.google.guiceberry.GuiceBerryUniverse.TestCaseScaffolding;
import com.google.inject.Module;

import org.junit.rules.MethodRule;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;

/**
 * @author Luiz-Otavio "Z" Zorzella
 */
public class GuiceBerryRule implements MethodRule {

  private final Class<? extends Module> envClass;

  public GuiceBerryRule(Class<? extends Module> envClass) {
    this.envClass = envClass;
  }

  public Statement apply(final Statement base, final FrameworkMethod method, final Object target) {
    return new Statement() {
      
      @Override
      public void evaluate() throws Throwable {
        TestCaseScaffolding toTearDown = 
          GuiceBerry.setup(buildTestDescription(target, method.getName()), envClass);
        try {
          base.evaluate();
        } finally {
          toTearDown.goTearDown();
        }
      }
    };
  }

  private static TestDescription buildTestDescription(Object testCase, String methodName) {
    String testCaseName = testCase.getClass().getName();
    return new TestDescription(testCase, testCaseName + "." + methodName,
      new TestId(testCaseName, methodName));
  }
}
