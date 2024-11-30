/*
 *  Copyright 2024 Mathias Bläsing
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
package org.teavm.flavour.rest.test;

import org.teavm.flavour.json.JsonPersistable;
import org.teavm.jso.JSObject;

/**
 *
 * @author matthias
 */
@JsonPersistable
public class ComplexDemo implements JSObject {
    @JsonPersistable
    public enum Status {
        CHOSEN,
        ORDERED,
        SENT,
        DELIVERED
    }

    private String stringDemo;
    private double doubleDemo;
    private Status status;
    private ComplexDemo nestedSemiComplexDemo;

    public String getStringDemo() {
        return stringDemo;
    }

    public void setStringDemo(String stringDemo) {
        this.stringDemo = stringDemo;
    }

    public double getDoubleDemo() {
        return doubleDemo;
    }

    public void setDoubleDemo(double doubleDemo) {
        this.doubleDemo = doubleDemo;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public ComplexDemo getNestedSemiComplexDemo() {
        return nestedSemiComplexDemo;
    }

    public void setNestedSemiComplexDemo(ComplexDemo nestedSemiComplexDemo) {
        this.nestedSemiComplexDemo = nestedSemiComplexDemo;
    }

    @Override
    public String toString() {
        return "SemiComplexDemo{" + "stringDemo=" + stringDemo + ", doubleDemo=" + doubleDemo + ", nestedSemiComplexDemo=" + nestedSemiComplexDemo + '}';
    }

}
