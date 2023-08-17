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
package org.teavm.flavour.templates.tree;

import org.teavm.flavour.expr.type.ValueType;

public class ComponentVariableBinding extends ComponentPropertyBinding {
    private ValueType rawValueType;
    private ValueType valueType;
    private String name;

    public ComponentVariableBinding(String methodOwner, String methodName, String name, ValueType rawValueType,
            ValueType valueType) {
        super(methodOwner, methodName);
        this.name = name;
        this.rawValueType = rawValueType;
        this.valueType = valueType;
    }

    public ValueType getRawValueType() {
        return rawValueType;
    }

    public void setRawValueType(ValueType rawValueType) {
        this.rawValueType = rawValueType;
    }

    public ValueType getValueType() {
        return valueType;
    }

    public void setValueType(ValueType valueType) {
        this.valueType = valueType;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
