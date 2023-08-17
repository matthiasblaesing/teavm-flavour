/*
 *  Copyright 2015 Alexey Andreev.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.teavm.flavour.routing.metadata;

import org.teavm.flavour.regex.ast.Node;
import org.teavm.metaprogramming.ReflectClass;

public class ParameterDescriptor {
    int index;
    int javaIndex;
    String name;
    ParameterType type;
    ReflectClass<?> valueType;
    Node pattern;
    Node effectivePattern;

    public ParameterDescriptor(int javaIndex, String name) {
        this.javaIndex = javaIndex;
        this.name = name;
    }

    public ParameterType getType() {
        return type;
    }

    public int getIndex() {
        return index;
    }

    public int getJavaIndex() {
        return javaIndex;
    }

    public String getName() {
        return name;
    }

    public Node getPattern() {
        return pattern;
    }

    public Node getEffectivePattern() {
        return effectivePattern;
    }

    public ReflectClass<?> getValueType() {
        return valueType;
    }
}
