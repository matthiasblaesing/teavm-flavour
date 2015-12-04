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
package org.teavm.flavour.mp.impl;

import java.util.List;
import java.util.stream.Collectors;
import org.teavm.model.BasicBlock;
import org.teavm.model.BasicBlockReader;
import org.teavm.model.FieldReference;
import org.teavm.model.Incoming;
import org.teavm.model.IncomingReader;
import org.teavm.model.Instruction;
import org.teavm.model.InstructionLocation;
import org.teavm.model.InvokeDynamicInstruction;
import org.teavm.model.MethodDescriptor;
import org.teavm.model.MethodHandle;
import org.teavm.model.MethodReference;
import org.teavm.model.Phi;
import org.teavm.model.PhiReader;
import org.teavm.model.Program;
import org.teavm.model.ProgramReader;
import org.teavm.model.RuntimeConstant;
import org.teavm.model.TryCatchBlock;
import org.teavm.model.TryCatchBlockReader;
import org.teavm.model.ValueType;
import org.teavm.model.Variable;
import org.teavm.model.VariableReader;
import org.teavm.model.instructions.ArrayElementType;
import org.teavm.model.instructions.ArrayLengthInstruction;
import org.teavm.model.instructions.AssignInstruction;
import org.teavm.model.instructions.BinaryBranchingCondition;
import org.teavm.model.instructions.BinaryBranchingInstruction;
import org.teavm.model.instructions.BinaryInstruction;
import org.teavm.model.instructions.BinaryOperation;
import org.teavm.model.instructions.BranchingCondition;
import org.teavm.model.instructions.BranchingInstruction;
import org.teavm.model.instructions.CastInstruction;
import org.teavm.model.instructions.CastIntegerDirection;
import org.teavm.model.instructions.CastIntegerInstruction;
import org.teavm.model.instructions.CastNumberInstruction;
import org.teavm.model.instructions.ClassConstantInstruction;
import org.teavm.model.instructions.CloneArrayInstruction;
import org.teavm.model.instructions.ConstructArrayInstruction;
import org.teavm.model.instructions.ConstructInstruction;
import org.teavm.model.instructions.ConstructMultiArrayInstruction;
import org.teavm.model.instructions.DoubleConstantInstruction;
import org.teavm.model.instructions.FloatConstantInstruction;
import org.teavm.model.instructions.GetElementInstruction;
import org.teavm.model.instructions.GetFieldInstruction;
import org.teavm.model.instructions.InitClassInstruction;
import org.teavm.model.instructions.InstructionReader;
import org.teavm.model.instructions.IntegerConstantInstruction;
import org.teavm.model.instructions.IntegerSubtype;
import org.teavm.model.instructions.InvocationType;
import org.teavm.model.instructions.InvokeInstruction;
import org.teavm.model.instructions.IsInstanceInstruction;
import org.teavm.model.instructions.JumpInstruction;
import org.teavm.model.instructions.LongConstantInstruction;
import org.teavm.model.instructions.MonitorEnterInstruction;
import org.teavm.model.instructions.MonitorExitInstruction;
import org.teavm.model.instructions.NegateInstruction;
import org.teavm.model.instructions.NullCheckInstruction;
import org.teavm.model.instructions.NullConstantInstruction;
import org.teavm.model.instructions.NumericOperandType;
import org.teavm.model.instructions.PutElementInstruction;
import org.teavm.model.instructions.PutFieldInstruction;
import org.teavm.model.instructions.RaiseInstruction;
import org.teavm.model.instructions.StringConstantInstruction;
import org.teavm.model.instructions.SwitchInstruction;
import org.teavm.model.instructions.SwitchTableEntry;
import org.teavm.model.instructions.SwitchTableEntryReader;
import org.teavm.model.instructions.UnwrapArrayInstruction;

/**
 *
 * @author Alexey Andreev
 */
public class CompoundMethodGenerator {
    Program program = new Program();
    private InstructionLocation location;
    private int blockIndex;
    private Variable resultVar;
    private Phi resultPhi;

    public CompoundMethodGenerator() {
        program.createBasicBlock();
    }

    public void addProgram(ProgramReader template, List<Object> capturedValues) {
        resultVar = null;
        resultPhi = null;
        blockIndex = program.basicBlockCount() - 1;
        List<Variable> capturedVars = capturedValues.stream().map(this::captureValue).collect(Collectors.toList());
        TemplateSubstitutor substitutor = new TemplateSubstitutor(capturedVars, template.basicBlockCount() - 1,
                program.variableCount() - capturedVars.size());

        for (int i = 0; i < template.basicBlockCount(); ++i) {
            program.createBasicBlock();
        }
        for (int i = capturedVars.size(); i < template.variableCount(); ++i) {
            program.createVariable();
        }

        for (int i = 0; i < template.basicBlockCount(); ++i) {
            BasicBlockReader templateBlock = template.basicBlockAt(i);
            blockIndex = substitutor.blockOffset + i;
            BasicBlock targetBlock = program.basicBlockAt(blockIndex);

            for (PhiReader templatePhi : templateBlock.readPhis()) {
                Phi phi = new Phi();
                for (IncomingReader templateIncoming : templatePhi.readIncomings()) {
                    Incoming incoming = new Incoming();
                    incoming.setSource(substitutor.block(templateIncoming.getSource()));
                    incoming.setValue(substitutor.var(templateIncoming.getValue()));
                    phi.getIncomings().add(incoming);
                }
                phi.setReceiver(substitutor.var(templatePhi.getReceiver()));
                targetBlock.getPhis().add(phi);
            }

            for (TryCatchBlockReader templateTryCatch : templateBlock.readTryCatchBlocks()) {
                TryCatchBlock tryCatch = new TryCatchBlock();
                tryCatch.setExceptionType(templateTryCatch.getExceptionType());
                tryCatch.setExceptionVariable(substitutor.var(templateTryCatch.getExceptionVariable()));
                tryCatch.setHandler(substitutor.block(templateTryCatch.getHandler()));
                targetBlock.getTryCatchBlocks().add(tryCatch);
            }

            templateBlock.readAllInstructions(substitutor);
        }
    }

    public Variable getResultVar() {
        return resultVar;
    }

    void add(Instruction insn) {
        insn.setLocation(location);
        program.basicBlockAt(blockIndex).getInstructions().add(insn);
    }

    private Variable captureValue(Object value) {
        if (value == null) {
            NullConstantInstruction insn = new NullConstantInstruction();
            insn.setReceiver(program.createVariable());
            add(insn);
            return insn.getReceiver();
        } else if (value instanceof Integer) {
            IntegerConstantInstruction insn = new IntegerConstantInstruction();
            insn.setReceiver(program.createVariable());
            insn.setConstant((Integer) value);
            add(insn);
            return insn.getReceiver();
        } else if (value instanceof Long) {
            LongConstantInstruction insn = new LongConstantInstruction();
            insn.setReceiver(program.createVariable());
            insn.setConstant((Long) value);
            add(insn);
            return insn.getReceiver();
        } else if (value instanceof Float) {
            FloatConstantInstruction insn = new FloatConstantInstruction();
            insn.setReceiver(program.createVariable());
            insn.setConstant((Float) value);
            add(insn);
            return insn.getReceiver();
        } else if (value instanceof Double) {
            DoubleConstantInstruction insn = new DoubleConstantInstruction();
            insn.setReceiver(program.createVariable());
            insn.setConstant((Double) value);
            add(insn);
            return insn.getReceiver();
        } else if (value instanceof String) {
            StringConstantInstruction insn = new StringConstantInstruction();
            insn.setReceiver(program.createVariable());
            insn.setConstant((String) value);
            add(insn);
            return insn.getReceiver();
        } else if (value instanceof ValueType) {
            ClassConstantInstruction insn = new ClassConstantInstruction();
            insn.setReceiver(program.createVariable());
            insn.setConstant((ValueType) value);
            add(insn);
            return insn.getReceiver();
        } else if (value instanceof ValueImpl) {
            return ((ValueImpl<?>) value).innerValue;
        } else {
            throw new WrongCapturedValueException();
        }
    }

    public Program getProgram() {
        return program;
    }

    private class TemplateSubstitutor implements InstructionReader {
        private int blockOffset;
        private int variableOffset;
        List<Variable> capturedVars;

        public TemplateSubstitutor(List<Variable> capturedVars, int blockOffset, int variableOffset) {
            this.capturedVars = capturedVars;
            this.blockOffset = blockOffset;
            this.variableOffset = variableOffset;
        }

        @Override
        public void location(InstructionLocation location) {
            CompoundMethodGenerator.this.location = location;
        }

        @Override
        public void nop() {
        }

        public Variable var(VariableReader variable) {
            if (variable == null) {
                return null;
            }
            if (variable.getIndex() < capturedVars.size()) {
                return capturedVars.get(variable.getIndex());
            }
            return program.variableAt(variableOffset + variable.getIndex());
        }

        public BasicBlock block(BasicBlockReader block) {
            if (block == null) {
                return null;
            }
            return program.basicBlockAt(blockOffset + block.getIndex());
        }

        @Override
        public void classConstant(VariableReader receiver, ValueType cst) {
            ClassConstantInstruction insn = new ClassConstantInstruction();
            insn.setConstant(cst);
            insn.setReceiver(var(receiver));
            add(insn);
        }

        @Override
        public void nullConstant(VariableReader receiver) {
            NullConstantInstruction insn = new NullConstantInstruction();
            insn.setReceiver(var(receiver));
            add(insn);
        }

        @Override
        public void integerConstant(VariableReader receiver, int cst) {
            IntegerConstantInstruction insn = new IntegerConstantInstruction();
            insn.setConstant(cst);
            insn.setReceiver(var(receiver));
            add(insn);
        }

        @Override
        public void longConstant(VariableReader receiver, long cst) {
            LongConstantInstruction insn = new LongConstantInstruction();
            insn.setConstant(cst);
            insn.setReceiver(var(receiver));
            add(insn);
        }

        @Override
        public void floatConstant(VariableReader receiver, float cst) {
            FloatConstantInstruction insn = new FloatConstantInstruction();
            insn.setConstant(cst);
            insn.setReceiver(var(receiver));
            add(insn);
        }

        @Override
        public void doubleConstant(VariableReader receiver, double cst) {
            DoubleConstantInstruction insn = new DoubleConstantInstruction();
            insn.setConstant(cst);
            insn.setReceiver(var(receiver));
            add(insn);
        }

        @Override
        public void stringConstant(VariableReader receiver, String cst) {
            StringConstantInstruction insn = new StringConstantInstruction();
            insn.setConstant(cst);
            insn.setReceiver(var(receiver));
            add(insn);
        }

        @Override
        public void binary(BinaryOperation op, VariableReader receiver, VariableReader first, VariableReader second,
                NumericOperandType type) {
            BinaryInstruction insn = new BinaryInstruction(op, type);
            insn.setReceiver(var(receiver));
            insn.setFirstOperand(var(first));
            insn.setSecondOperand(var(second));
            add(insn);
        }

        @Override
        public void negate(VariableReader receiver, VariableReader operand, NumericOperandType type) {
            NegateInstruction insn = new NegateInstruction(type);
            insn.setReceiver(var(receiver));
            insn.setOperand(var(operand));
            add(insn);
        }

        @Override
        public void assign(VariableReader receiver, VariableReader assignee) {
            AssignInstruction insn = new AssignInstruction();
            insn.setReceiver(var(receiver));
            insn.setAssignee(var(assignee));
            add(insn);
        }

        @Override
        public void cast(VariableReader receiver, VariableReader value, ValueType targetType) {
            CastInstruction insn = new CastInstruction();
            insn.setTargetType(targetType);
            insn.setValue(var(value));
            insn.setReceiver(var(receiver));
            add(insn);
        }

        @Override
        public void cast(VariableReader receiver, VariableReader value, NumericOperandType sourceType,
                NumericOperandType targetType) {
            CastNumberInstruction insn = new CastNumberInstruction(sourceType, targetType);
            insn.setValue(var(value));
            insn.setReceiver(var(receiver));
            add(insn);
        }

        @Override
        public void cast(VariableReader receiver, VariableReader value, IntegerSubtype type,
                CastIntegerDirection targetType) {
            CastIntegerInstruction insn = new CastIntegerInstruction(type, targetType);
            insn.setValue(var(value));
            insn.setReceiver(var(receiver));
            add(insn);
        }

        @Override
        public void jumpIf(BranchingCondition cond, VariableReader operand, BasicBlockReader consequent,
                BasicBlockReader alternative) {
            BranchingInstruction insn = new BranchingInstruction(cond);
            insn.setOperand(var(operand));
            insn.setConsequent(block(consequent));
            insn.setAlternative(block(alternative));
            add(insn);
        }

        @Override
        public void jumpIf(BinaryBranchingCondition cond, VariableReader first, VariableReader second,
                BasicBlockReader consequent, BasicBlockReader alternative) {
            BinaryBranchingInstruction insn = new BinaryBranchingInstruction(cond);
            insn.setFirstOperand(var(first));
            insn.setSecondOperand(var(second));
            insn.setConsequent(block(consequent));
            insn.setAlternative(block(alternative));
            add(insn);
        }

        @Override
        public void jump(BasicBlockReader target) {
            JumpInstruction insn = new JumpInstruction();
            insn.setTarget(block(target));
        }

        @Override
        public void choose(VariableReader condition, List<? extends SwitchTableEntryReader> table,
                BasicBlockReader defaultTarget) {
            SwitchInstruction insn = new SwitchInstruction();
            insn.setCondition(var(condition));
            insn.setDefaultTarget(block(defaultTarget));
            for (SwitchTableEntryReader entry : table) {
                SwitchTableEntry insnEntry = new SwitchTableEntry();
                insnEntry.setCondition(entry.getCondition());
                insnEntry.setTarget(block(entry.getTarget()));
                insn.getEntries().add(insnEntry);
            }
            add(insn);
        }

        @Override
        public void exit(VariableReader valueToReturn) {
            BasicBlock target = program.basicBlockAt(program.basicBlockCount() - 1);

            if (valueToReturn != null) {
                if (resultVar == null) {
                    resultVar = program.createVariable();
                    resultPhi = new Phi();
                    resultPhi.setReceiver(resultVar);
                    target.getPhis().add(resultPhi);
                }
                Incoming incoming = new Incoming();
                incoming.setSource(program.basicBlockAt(blockIndex));
                incoming.setValue(var(valueToReturn));
                resultPhi.getIncomings().add(incoming);
            }

            JumpInstruction insn = new JumpInstruction();
            insn.setTarget(target);
            add(insn);
        }

        @Override
        public void raise(VariableReader exception) {
            RaiseInstruction insn = new RaiseInstruction();
            insn.setException(var(exception));
            add(insn);
        }

        @Override
        public void createArray(VariableReader receiver, ValueType itemType, VariableReader size) {
            ConstructArrayInstruction insn = new ConstructArrayInstruction();
            insn.setReceiver(var(receiver));
            insn.setItemType(itemType);
            insn.setSize(var(size));
            add(insn);
        }

        @Override
        public void createArray(VariableReader receiver, ValueType itemType,
                List<? extends VariableReader> dimensions) {
            ConstructMultiArrayInstruction insn = new ConstructMultiArrayInstruction();
            insn.setReceiver(var(receiver));
            insn.setItemType(itemType);
            insn.getDimensions().addAll(dimensions.stream().map(this::var).collect(Collectors.toList()));
            add(insn);
        }

        @Override
        public void create(VariableReader receiver, String type) {
            ConstructInstruction insn = new ConstructInstruction();
            insn.setReceiver(var(receiver));
            insn.setType(type);
            add(insn);
        }

        @Override
        public void getField(VariableReader receiver, VariableReader instance, FieldReference field,
                ValueType fieldType) {
            GetFieldInstruction insn = new GetFieldInstruction();
            insn.setField(field);
            insn.setFieldType(fieldType);
            insn.setInstance(var(instance));
            insn.setReceiver(var(receiver));
            add(insn);
        }

        @Override
        public void putField(VariableReader instance, FieldReference field, VariableReader value,
                ValueType fieldType) {
            PutFieldInstruction insn = new PutFieldInstruction();
            insn.setField(field);
            insn.setFieldType(fieldType);
            insn.setInstance(var(instance));
            insn.setValue(var(value));
            add(insn);
        }

        @Override
        public void arrayLength(VariableReader receiver, VariableReader array) {
            ArrayLengthInstruction insn = new ArrayLengthInstruction();
            insn.setArray(var(array));
            insn.setReceiver(var(receiver));
            add(insn);
        }

        @Override
        public void cloneArray(VariableReader receiver, VariableReader array) {
            CloneArrayInstruction insn = new CloneArrayInstruction();
            insn.setArray(var(array));
            insn.setReceiver(var(receiver));
            add(insn);
        }

        @Override
        public void unwrapArray(VariableReader receiver, VariableReader array, ArrayElementType elementType) {
            UnwrapArrayInstruction insn = new UnwrapArrayInstruction(elementType);
            insn.setArray(var(array));
            insn.setReceiver(var(receiver));
            add(insn);
        }

        @Override
        public void getElement(VariableReader receiver, VariableReader array, VariableReader index) {
            GetElementInstruction insn = new GetElementInstruction();
            insn.setArray(var(array));
            insn.setIndex(var(index));
            insn.setReceiver(var(receiver));
            add(insn);
        }

        @Override
        public void putElement(VariableReader array, VariableReader index, VariableReader value) {
            PutElementInstruction insn = new PutElementInstruction();
            insn.setArray(var(array));
            insn.setIndex(var(index));
            insn.setValue(var(value));
            add(insn);
        }

        @Override
        public void invoke(VariableReader receiver, VariableReader instance, MethodReference method,
                List<? extends VariableReader> arguments, InvocationType type) {
            InvokeInstruction insn = new InvokeInstruction();
            insn.setInstance(var(instance));
            insn.setReceiver(var(receiver));
            insn.setMethod(method);
            insn.setType(type);
            insn.getArguments().addAll(arguments.stream().map(this::var).collect(Collectors.toList()));
            add(insn);
        }

        @Override
        public void invokeDynamic(VariableReader receiver, VariableReader instance, MethodDescriptor method,
                List<? extends VariableReader> arguments, MethodHandle bootstrapMethod,
                List<RuntimeConstant> bootstrapArguments) {
            InvokeDynamicInstruction insn = new InvokeDynamicInstruction();
            insn.setBootstrapMethod(bootstrapMethod);
            insn.setInstance(var(instance));
            insn.setReceiver(var(receiver));
            insn.getArguments().addAll(arguments.stream().map(this::var).collect(Collectors.toList()));
            insn.getBootstrapArguments().addAll(bootstrapArguments);
            add(insn);
        }

        @Override
        public void isInstance(VariableReader receiver, VariableReader value, ValueType type) {
            IsInstanceInstruction insn = new IsInstanceInstruction();
            insn.setReceiver(var(receiver));
            insn.setValue(var(value));
            insn.setType(type);
            add(insn);
        }

        @Override
        public void initClass(String className) {
            InitClassInstruction insn = new InitClassInstruction();
            insn.setClassName(className);
            add(insn);
        }

        @Override
        public void nullCheck(VariableReader receiver, VariableReader value) {
            NullCheckInstruction insn = new NullCheckInstruction();
            insn.setReceiver(var(receiver));
            insn.setValue(var(value));
            add(insn);
        }

        @Override
        public void monitorEnter(VariableReader objectRef) {
            MonitorEnterInstruction insn = new MonitorEnterInstruction();
            insn.setObjectRef(var(objectRef));
            add(insn);
        }

        @Override
        public void monitorExit(VariableReader objectRef) {
            MonitorExitInstruction insn = new MonitorExitInstruction();
            insn.setObjectRef(var(objectRef));
            add(insn);
        }
    }
}
