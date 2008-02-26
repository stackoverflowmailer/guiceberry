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

package com.google.inject.testing.guiceberry;

import com.google.inject.Key;
import com.google.inject.Injector;
import com.google.inject.AbstractModule;
import com.google.inject.commands.intercepting.InterceptingInjectorBuilder;
import com.google.inject.name.Names;

import junit.framework.TestCase;

import java.util.ArrayList;

/**
 * @author zorzella
 * @author jessewilson@google.com (Jesse Wilson)
 */
public class InjectionControllerTest extends TestCase {

  private InjectionController injectionController = new InjectionController();

  public void testCantOverrideDouble() throws Exception {
    injectionController.substitute(String.class, "foo");
    assertEquals("foo", injectionController.getSubstitute(String.class));
    try {
      injectionController.substitute(String.class, "bar");
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }
  
  public void testKeyInjection() {
    Key<String> stringNamedTen = Key.get(String.class, Names.named("ten"));
    injectionController.substitute(stringNamedTen, "10");
    assertNull(injectionController.getSubstitute(String.class));
    assertEquals("10", injectionController.getSubstitute(stringNamedTen));
  }

  public void testSimpleOverride() throws Exception {
    Injector injector = new InterceptingInjectorBuilder()
        .install(injectionController.createModule(),
            new AbstractModule() {
              protected void configure() {
                bind(String.class).toInstance("a");
              }
            })
        .intercept(String.class)
        .build();

    assertEquals("a", injector.getInstance(String.class));
    injectionController.substitute(String.class, "b");
    assertEquals("b", injector.getInstance(String.class));
  }

  public void testOverrideRequiresWhitelist() throws Exception {
    Injector injector = new InterceptingInjectorBuilder()
        .install(injectionController.createModule(),
            new AbstractModule() {
              protected void configure() {
                bind(String.class).toInstance("a");
              }
            })
        .build();

    injectionController.substitute(String.class, "b");
    assertEquals("a", injector.getInstance(String.class));
  }

  public void testBareBindingFails() throws Exception {
    InterceptingInjectorBuilder builder = new InterceptingInjectorBuilder()
        .install(injectionController.createModule(),
            new AbstractModule() {
              protected void configure() {
                bind(ArrayList.class);
              }
            })
        .intercept(ArrayList.class);

    try {
      builder.build();
      fail();
    } catch (UnsupportedOperationException expected) {
    }
  }
}
