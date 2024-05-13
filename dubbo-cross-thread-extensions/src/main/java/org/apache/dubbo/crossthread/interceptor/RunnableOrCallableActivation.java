/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.dubbo.crossthread.interceptor;

import org.apache.dubbo.crossthread.toolkit.DubboCrossThread;

import java.lang.instrument.Instrumentation;

import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.matcher.ElementMatchers;

import static net.bytebuddy.matcher.ElementMatchers.isAnnotatedWith;
import static net.bytebuddy.matcher.ElementMatchers.takesArguments;

public class RunnableOrCallableActivation {
    // add '_' before dubboTag to avoid conflict field name
    public static final String FIELD_NAME_DUBBO_TAG = "_dubboTag";
    private static final String CALL_METHOD_NAME = "call";
    private static final String RUN_METHOD_NAME = "run";
    private static final String APPLY_METHOD_NAME = "apply";
    private static final String ACCEPT_METHOD_NAME = "accept";

    public static void install(Instrumentation instrumentation) {
        new AgentBuilder.Default()
            .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
            .with(AgentBuilder.TypeStrategy.Default.REBASE)
            .with(AgentBuilder.RedefinitionStrategy.REDEFINITION)
            .type(isAnnotatedWith(DubboCrossThread.class))
            .transform((builder, typeDescription, classLoader, module, protectionDomain) -> builder
                .defineField(FIELD_NAME_DUBBO_TAG, String.class, Visibility.PUBLIC)
                .visit(Advice.to(RunnableOrCallableMethodInterceptor.class).on(
                    ElementMatchers.isMethod().and(
                        ElementMatchers.named(RUN_METHOD_NAME).and(takesArguments(0))
                            .or(ElementMatchers.named(CALL_METHOD_NAME).and(takesArguments(0)))
                            .or(ElementMatchers.named(APPLY_METHOD_NAME).and(takesArguments(0)))
                            .or(ElementMatchers.named(ACCEPT_METHOD_NAME).and(takesArguments(0)))
                    )
                ))
                .visit(Advice.to(RunnableOrCallableConstructInterceptor.class).on(
                    ElementMatchers.isConstructor()
                )))
            .installOn(instrumentation);
    }
}
