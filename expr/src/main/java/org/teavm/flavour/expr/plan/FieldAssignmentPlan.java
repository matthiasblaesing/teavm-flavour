/*
 *  Copyright 2016 Alexey Andreev.
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
package org.teavm.flavour.expr.plan;

public class FieldAssignmentPlan extends Plan {
    private Plan instance;
    private String className;
    private String fieldName;
    private String fieldDesc;
    private Plan value;

    public FieldAssignmentPlan(Plan instance, String className, String fieldName, String fieldDesc, Plan value) {
        this.instance = instance;
        this.className = className;
        this.fieldName = fieldName;
        this.fieldDesc = fieldDesc;
        this.value = value;
    }

    public Plan getInstance() {
        return instance;
    }

    public void setInstance(Plan instance) {
        this.instance = instance;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public String getFieldDesc() {
        return fieldDesc;
    }

    public void setFieldDesc(String fieldDesc) {
        this.fieldDesc = fieldDesc;
    }

    public Plan getValue() {
        return value;
    }

    public void setValue(Plan value) {
        this.value = value;
    }

    @Override
    public void acceptVisitor(PlanVisitor visitor) {
        visitor.visit(this);
    }
}
