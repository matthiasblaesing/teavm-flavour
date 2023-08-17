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
package org.teavm.flavour.expr.plan;

public class ReferenceEqualityPlan extends Plan {
    private Plan firstOperand;
    private Plan secondOperand;
    private ReferenceEqualityPlanType type;

    public ReferenceEqualityPlan(Plan firstOperand, Plan secondOperand, ReferenceEqualityPlanType type) {
        this.firstOperand = firstOperand;
        this.secondOperand = secondOperand;
        this.type = type;
    }

    public Plan getFirstOperand() {
        return firstOperand;
    }

    public void setFirstOperand(Plan firstOperand) {
        this.firstOperand = firstOperand;
    }

    public Plan getSecondOperand() {
        return secondOperand;
    }

    public void setSecondOperand(Plan secondOperand) {
        this.secondOperand = secondOperand;
    }

    public ReferenceEqualityPlanType getType() {
        return type;
    }

    public void setType(ReferenceEqualityPlanType type) {
        this.type = type;
    }

    @Override
    public void acceptVisitor(PlanVisitor visitor) {
        visitor.visit(this);
    }
}
