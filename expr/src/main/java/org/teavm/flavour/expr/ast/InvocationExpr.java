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
package org.teavm.flavour.expr.ast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class InvocationExpr extends Expr {
    private Expr instance;
    private String methodName;
    private List<Expr> arguments = new ArrayList<>();

    public InvocationExpr(Expr instance, String methodName, Expr... arguments) {
        this(instance, methodName, Arrays.asList(arguments));
    }

    public InvocationExpr(Expr instance, String methodName, Collection<Expr> arguments) {
        this.instance = instance;
        this.methodName = methodName;
        this.arguments.addAll(arguments);
    }

    public Expr getInstance() {
        return instance;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public void setInstance(Expr instance) {
        this.instance = instance;
    }

    public List<Expr> getArguments() {
        return arguments;
    }

    @Override
    public <T> T acceptVisitor(ExprVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
