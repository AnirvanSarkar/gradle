/*
 * Copyright 2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.internal.classpath.intercept;

import org.gradle.internal.classpath.GroovyCallInterceptorsProvider;
import org.gradle.internal.instrumentation.api.groovybytecode.CallInterceptor;
import org.gradle.internal.instrumentation.api.types.BytecodeInterceptorFilter;

import java.util.List;
import java.util.stream.Collectors;

public class DefaultCallSiteInterceptorSet implements CallSiteInterceptorSet {

    private final GroovyCallInterceptorsProvider provider;

    public DefaultCallSiteInterceptorSet(GroovyCallInterceptorsProvider provider) {
        this.provider = provider;
    }

    @Override
    public List<CallInterceptor> getCallInterceptors(BytecodeInterceptorFilter filter) {
        return provider.getCallInterceptors().stream()
            .filter(filter::matches)
            .collect(Collectors.toList());
    }
}
