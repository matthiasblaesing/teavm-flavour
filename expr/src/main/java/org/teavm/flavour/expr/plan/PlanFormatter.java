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

public class PlanFormatter implements PlanVisitor {
    private StringBuilder sb;
    private int indentLevel;

    public PlanFormatter(StringBuilder sb) {
        this.sb = sb;
    }

    @Override
    public void visit(ConstantPlan plan) {
        sb.append("[constant ").append(plan.getValue()).append("]");
    }

    @Override
    public void visit(VariablePlan plan) {
        sb.append("[var ").append(plan.getName()).append("]");
    }

    @Override
    public void visit(BinaryPlan plan) {
        sb.append("[")
                .append(plan.getType().name().toLowerCase()).append('-')
                .append(plan.getValueType().name().toLowerCase());
        ++indentLevel;
        newLine();
        plan.getFirstOperand().acceptVisitor(this);
        newLine();
        plan.getSecondOperand().acceptVisitor(this);
        sb.append(']');
        --indentLevel;
    }

    @Override
    public void visit(NegatePlan plan) {
        sb.append("[negate-").append(plan.getValueType().name().toLowerCase());
        ++indentLevel;
        newLine();
        plan.getOperand().acceptVisitor(this);
        sb.append(']');
        --indentLevel;
    }

    @Override
    public void visit(ReferenceEqualityPlan plan) {
        sb.append("[").append(plan.getType().name().toLowerCase()).append("-ref");
        ++indentLevel;
        newLine();
        plan.getFirstOperand().acceptVisitor(this);
        newLine();
        plan.getSecondOperand().acceptVisitor(this);
        sb.append(']');
        --indentLevel;
    }

    @Override
    public void visit(LogicalBinaryPlan plan) {
        sb.append("[").append(plan.getType().name().toLowerCase());
        ++indentLevel;
        newLine();
        plan.getFirstOperand().acceptVisitor(this);
        newLine();
        plan.getSecondOperand().acceptVisitor(this);
        sb.append(']');
        --indentLevel;
    }

    @Override
    public void visit(NotPlan plan) {
        sb.append("[not");
        ++indentLevel;
        newLine();
        plan.getOperand().acceptVisitor(this);
        sb.append(']');
        --indentLevel;
    }

    @Override
    public void visit(CastPlan plan) {
        sb.append("[cast-obj-to ").append(plan.getTargetType());
        ++indentLevel;
        newLine();
        plan.getOperand().acceptVisitor(this);
        sb.append(']');
        --indentLevel;
    }

    @Override
    public void visit(ArithmeticCastPlan plan) {
        sb.append("[cast-").append(plan.getSourceType().name().toLowerCase()).append("-to-")
                .append(plan.getTargetType().name().toLowerCase());
        ++indentLevel;
        newLine();
        plan.getOperand().acceptVisitor(this);
        sb.append(']');
        --indentLevel;
    }

    @Override
    public void visit(CastFromIntegerPlan plan) {
        sb.append("[cast-int-to-").append(plan.getType().name().toLowerCase());
        ++indentLevel;
        newLine();
        plan.getOperand().acceptVisitor(this);
        sb.append(']');
        --indentLevel;
    }

    @Override
    public void visit(CastToIntegerPlan plan) {
        sb.append("[cast-").append(plan.getType().name().toLowerCase()).append("-to-int");
        ++indentLevel;
        newLine();
        plan.getOperand().acceptVisitor(this);
        sb.append(']');
        --indentLevel;
    }

    @Override
    public void visit(GetArrayElementPlan plan) {
        sb.append("[array-subscript ");
        ++indentLevel;
        newLine();
        plan.getArray().acceptVisitor(this);
        newLine();
        plan.getIndex().acceptVisitor(this);
        sb.append(']');
        --indentLevel;
    }

    @Override
    public void visit(ArrayLengthPlan plan) {
        sb.append("[array-length ");
        ++indentLevel;
        newLine();
        plan.getArray().acceptVisitor(this);
        sb.append(']');
        --indentLevel;
    }

    @Override
    public void visit(FieldPlan plan) {
        sb.append("[get-field");
        if (plan.getInstance() == null) {
            sb.append("-static");
        }
        sb.append(" ").append(plan.getClassName()).append('.').append(plan.getFieldName());
        if (plan.getInstance() != null) {
            ++indentLevel;
            newLine();
            plan.getInstance().acceptVisitor(this);
            sb.append(']');
            --indentLevel;
        }
    }

    @Override
    public void visit(FieldAssignmentPlan plan) {
        sb.append("[set-field");
        if (plan.getInstance() == null) {
            sb.append("-static");
        }
        sb.append(" ").append(plan.getClassName()).append('.').append(plan.getFieldName());
        ++indentLevel;
        if (plan.getInstance() != null) {
            newLine();
            plan.getInstance().acceptVisitor(this);
        }
        newLine();
        plan.getValue().acceptVisitor(this);
        sb.append(']');
        --indentLevel;
    }

    @Override
    public void visit(InstanceOfPlan plan) {
        sb.append("[instanceof ").append(plan.getClassName());
        ++indentLevel;
        newLine();
        plan.getOperand().acceptVisitor(this);
        --indentLevel;
    }

    @Override
    public void visit(InvocationPlan plan) {
        sb.append("[invoke-method");
        if (plan.getInstance() == null) {
            sb.append("-static");
        }
        sb.append(" ").append(plan.getClassName()).append('.').append(plan.getMethodName())
                .append(plan.getMethodDesc());
        ++indentLevel;
        if (plan.getInstance() != null) {
            newLine();
            plan.getInstance().acceptVisitor(this);
        }
        for (Plan arg : plan.getArguments()) {
            newLine();
            arg.acceptVisitor(this);
        }
        sb.append(']');
        --indentLevel;
    }

    @Override
    public void visit(ConstructionPlan plan) {
        sb.append("[instantiate ");
        sb.append(" ").append(plan.getClassName()).append(".<init>").append(plan.getMethodDesc());
        ++indentLevel;
        for (Plan arg : plan.getArguments()) {
            newLine();
            arg.acceptVisitor(this);
        }
        sb.append(']');
        --indentLevel;
    }

    @Override
    public void visit(ArrayConstructionPlan plan) {
        sb.append("[new-array ");
        sb.append(plan.getElementType());
        ++indentLevel;
        for (Plan elem : plan.getElements()) {
            newLine();
            elem.acceptVisitor(this);
        }
        sb.append(']');
        --indentLevel;
    }

    @Override
    public void visit(ConditionalPlan plan) {
        sb.append("[if");
        ++indentLevel;
        newLine();
        plan.getCondition().acceptVisitor(this);
        newLine();
        plan.getConsequent().acceptVisitor(this);
        newLine();
        plan.getAlternative().acceptVisitor(this);
        sb.append(']');
        --indentLevel;
    }

    @Override
    public void visit(ThisPlan plan) {
        sb.append("[this]");
    }

    @Override
    public void visit(LambdaPlan plan) {
        sb.append("[lambda (");
        for (int i = 0; i < plan.getBoundVars().size(); ++i) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(plan.getBoundVars().get(i));
        }
        sb.append(")");
        ++indentLevel;
        newLine();
        sb.append(plan.getClassName()).append("#").append(plan.getMethodName()).append(plan.getMethodDesc());
        newLine();
        plan.getBody().acceptVisitor(this);
        sb.append(']');
        --indentLevel;
    }

    @Override
    public void visit(ObjectPlan plan) {
        sb.append("[object ").append(plan.getClassName());
        ++indentLevel;
        boolean first = true;
        for (ObjectPlanEntry entry : plan.getEntries()) {
            if (!first) {
                sb.append(",");
            }
            first = false;
            newLine();
            sb.append("[entry ").append(entry.getSetterName()).append(entry.getSetterDesc());
            entry.getValue().acceptVisitor(this);
            sb.append("]");
        }
        sb.append("]");
        --indentLevel;
    }

    private void printIndent() {
        for (int i = 0; i < indentLevel; ++i) {
            sb.append("  ");
        }
    }

    private void newLine() {
        sb.append('\n');
        printIndent();
    }
}
