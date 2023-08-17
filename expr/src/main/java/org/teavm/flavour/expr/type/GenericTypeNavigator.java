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
package org.teavm.flavour.expr.type;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.teavm.flavour.expr.type.meta.ClassDescriber;
import org.teavm.flavour.expr.type.meta.ClassDescriberRepository;
import org.teavm.flavour.expr.type.meta.FieldDescriber;
import org.teavm.flavour.expr.type.meta.MethodDescriber;

public class GenericTypeNavigator {
    private ClassDescriberRepository classRepository;

    public GenericTypeNavigator(ClassDescriberRepository classRepository) {
        this.classRepository = classRepository;
    }

    public ClassDescriberRepository getClassRepository() {
        return classRepository;
    }

    public List<GenericClass> sublassPath(GenericClass subclass, String superclass) {
        List<GenericClass> path = new ArrayList<>();
        if (!subclassPathImpl(subclass, superclass, path)) {
            return null;
        }
        return path;
    }

    private boolean subclassPathImpl(GenericClass subclass, String superclass, List<GenericClass> path) {
        path.add(subclass);
        if (subclass.getName().equals(superclass)) {
            return true;
        }
        GenericClass parent = getParent(subclass);
        if (parent != null) {
            if (subclassPathImpl(parent, superclass, path)) {
                return true;
            }
        }
        for (GenericClass iface : getInterfaces(subclass)) {
            if (subclassPathImpl(iface, superclass, path)) {
                return true;
            }
        }
        path.remove(path.size() - 1);
        return false;
    }

    public Set<String> commonSupertypes(Set<String> firstSet, Set<String> secondSet) {
        Set<String> firstAncestors = allAncestors(firstSet);
        Set<String> commonSupertypes = new HashSet<>();
        Set<String> visited = new HashSet<>();
        for (String cls : secondSet) {
            commonSupertypesImpl(cls, firstAncestors, visited, commonSupertypes);
        }
        return commonSupertypes;
    }

    private void commonSupertypesImpl(String cls, Set<String> ancestors, Set<String> visited,
            Set<String> commonSupertypes) {
        if (!visited.add(cls)) {
            return;
        }
        if (ancestors.contains(cls)) {
            commonSupertypes.add(cls);
            return;
        }
        ClassDescriber desc = classRepository.describe(cls);
        if (desc == null) {
            return;
        }
        if (desc.getSupertype() != null) {
            commonSupertypesImpl(desc.getSupertype().getName(), ancestors, visited, commonSupertypes);
        }
        for (GenericClass iface : desc.getInterfaces()) {
            commonSupertypesImpl(iface.getName(), ancestors, visited, commonSupertypes);
        }
    }

    public Set<String> allAncestors(Collection<String> classes) {
        Set<String> ancestors = new HashSet<>();
        for (String cls : classes) {
            allAncestorsImpl(cls, ancestors);
        }
        return ancestors;
    }

    private void allAncestorsImpl(String cls, Set<String> ancestors) {
        if (!ancestors.add(cls)) {
            return;
        }
        ClassDescriber desc = classRepository.describe(cls);
        if (desc == null) {
            return;
        }
        if (desc.getSupertype() != null) {
            allAncestorsImpl(desc.getSupertype().getName(), ancestors);
        }
        for (GenericClass iface : desc.getInterfaces()) {
            allAncestorsImpl(iface.getName(), ancestors);
        }
    }

    public GenericClass getGenericClass(String className) {
        ClassDescriber describer = classRepository.describe(className);
        if (describer == null) {
            return null;
        }
        List<TypeArgument> arguments = new ArrayList<>();
        for (TypeVar var : describer.getTypeVariables()) {
            arguments.add(TypeArgument.invariant(new GenericReference(var)));
        }
        return new GenericClass(className, arguments);
    }

    public GenericClass getParent(GenericClass cls) {
        ClassDescriber describer = classRepository.describe(cls.getName());
        if (describer == null) {
            return null;
        }
        GenericClass superType = describer.getSupertype();
        if (superType == null) {
            return null;
        }

        TypeVar[] typeVars = describer.getTypeVariables();
        List<? extends TypeArgument> typeValues = cls.getArguments();
        if (typeVars.length != typeValues.size()) {
            return null;
        }
        Map<TypeVar, TypeArgument> substitutions = new HashMap<>();
        for (int i = 0; i < typeVars.length; ++i) {
            substitutions.put(typeVars[i], typeValues.get(i));
        }

        return superType.substituteArgs(substitutions::get);
    }

    public GenericClass[] getInterfaces(GenericClass cls) {
        ClassDescriber describer = classRepository.describe(cls.getName());
        if (describer == null) {
            return new GenericClass[0];
        }

        TypeVar[] typeVars = describer.getTypeVariables();
        List<? extends TypeArgument> typeValues = cls.getArguments();
        if (typeVars.length != typeValues.size()) {
            return new GenericClass[0];
        }
        Map<TypeVar, TypeArgument> substitutions = new HashMap<>();
        for (int i = 0; i < typeVars.length; ++i) {
            substitutions.put(typeVars[i], typeValues.get(i));
        }

        GenericClass[] interfaces = describer.getInterfaces();
        GenericClass[] result = new GenericClass[interfaces.length];
        for (int i = 0; i < interfaces.length; ++i) {
            result[i] = interfaces[i].substituteArgs(substitutions::get);
        }
        return result;
    }

    public GenericMethod[] findMethods(GenericClass cls, String name, int paramCount) {
        Map<MethodSignature, GenericMethod> methods = new HashMap<>();
        findMethodsImpl(cls, name, paramCount, new HashSet<>(), methods);
        return methods.values().toArray(new GenericMethod[0]);
    }

    private Map<TypeVar, TypeArgument> prepareSubstitutions(ClassDescriber describer, GenericClass cls) {
        TypeVar[] typeVars = describer.getTypeVariables();
        List<? extends TypeArgument> typeValues = cls.getArguments();
        if (typeVars.length != typeValues.size()) {
            return null;
        }
        Map<TypeVar, TypeArgument> substitutions = new HashMap<>();
        for (int i = 0; i < typeVars.length; ++i) {
            substitutions.put(typeVars[i], typeValues.get(i));
        }
        return substitutions;
    }

    public GenericField getField(GenericClass cls, String name) {
        return getFieldRec(cls, name, new HashSet<>());
    }

    private GenericField getFieldRec(GenericClass cls, String name, Set<GenericClass> visited) {
        if (!visited.add(cls)) {
            return null;
        }

        GenericField field = getFieldImpl(cls, name);
        if (field != null) {
            return field;
        }

        GenericClass parent = getParent(cls);
        if (parent != null) {
            field = getFieldRec(parent, name, visited);
            if (field != null) {
                return field;
            }
        }

        for (GenericClass iface : getInterfaces(cls)) {
            field = getFieldRec(iface, name, visited);
            if (field != null) {
                return field;
            }
        }

        return field;
    }

    private GenericField getFieldImpl(GenericClass cls, String name) {
        ClassDescriber describer = classRepository.describe(cls.getName());
        if (describer == null) {
            return null;
        }

        Map<TypeVar, TypeArgument> substitutions = prepareSubstitutions(describer, cls);
        if (substitutions == null) {
            return null;
        }

        FieldDescriber fieldDescriber = describer.getField(name);
        if (fieldDescriber == null) {
            return null;
        }

        ValueType type = fieldDescriber.getType();
        if (type instanceof GenericType) {
            type = ((GenericType) type).substituteArgs(substitutions::get);
        }
        return new GenericField(fieldDescriber, type);
    }

    public GenericMethod getMethod(GenericClass cls, String name, GenericClass... argumentTypes) {
        return getMethodRec(cls, name, argumentTypes, new HashSet<>());
    }

    private GenericMethod getMethodRec(GenericClass cls, String name, GenericClass[] argumentTypes,
            Set<GenericClass> visited) {
        if (!visited.add(cls)) {
            return null;
        }

        GenericMethod method = getMethodImpl(cls, name, argumentTypes);
        if (method != null) {
            return method;
        }

        GenericClass parent = getParent(cls);
        if (parent != null) {
            method = getMethodRec(parent, name, argumentTypes, visited);
            if (method != null) {
                return method;
            }
        }

        for (GenericClass iface : getInterfaces(cls)) {
            method = getMethodRec(iface, name, argumentTypes, visited);
            if (method != null) {
                return method;
            }
        }

        return method;
    }

    private GenericMethod getMethodImpl(GenericClass cls, String name, GenericClass... parameterTypes) {
        ClassDescriber describer = classRepository.describe(cls.getName());
        if (describer == null) {
            return null;
        }

        Map<TypeVar, TypeArgument> substitutions = prepareSubstitutions(describer, cls);
        if (substitutions == null) {
            return null;
        }

        MethodDescriber methodDescriber = describer.getMethod(name, parameterTypes);
        if (methodDescriber == null) {
            return null;
        }
        ValueType[] argTypes = methodDescriber.getParameterTypes();
        for (int i = 0; i < argTypes.length; ++i) {
            if (argTypes[i] instanceof GenericType) {
                argTypes[i] = ((GenericType) argTypes[i]).substituteArgs(substitutions::get);
            }
        }
        ValueType returnType = methodDescriber.getReturnType();
        if (returnType instanceof GenericType) {
            returnType = ((GenericType) returnType).substituteArgs(substitutions::get);
        }

        return new GenericMethod(methodDescriber, cls, parameterTypes, returnType);
    }

    private void findMethodsImpl(GenericClass cls, String name, int paramCount, Set<String> visitedClasses,
            Map<MethodSignature, GenericMethod> methods) {
        if (!visitedClasses.add(cls.getName())) {
            return;
        }

        ClassDescriber describer = classRepository.describe(cls.getName());
        if (describer == null) {
            return;
        }

        Map<TypeVar, TypeArgument> substitutions = prepareSubstitutions(describer, cls);
        if (substitutions == null) {
            return;
        }

        for (MethodDescriber methodDesc : describer.getMethods()) {
            if (!methodDesc.getName().equals(name)) {
                continue;
            }

            ValueType[] paramTypes = methodDesc.getParameterTypes();
            if (paramTypes.length != paramCount) {
                continue;
            }
            for (int i = 0; i < paramTypes.length; ++i) {
                if (paramTypes[i] instanceof GenericType) {
                    paramTypes[i] = ((GenericType) paramTypes[i]).substituteArgs(substitutions::get);
                }
            }

            ValueType returnType = methodDesc.getReturnType();
            if (returnType instanceof GenericType) {
                returnType = ((GenericType) returnType).substituteArgs(substitutions::get);
            }

            MethodSignature signature = new MethodSignature(methodDesc.getRawParameterTypes());
            methods.put(signature, new GenericMethod(methodDesc, cls, paramTypes, returnType));
        }

        GenericClass supertype = getParent(cls);
        if (supertype != null) {
            findMethodsImpl(supertype, name, paramCount, visitedClasses, methods);
        }
        for (GenericClass iface : getInterfaces(cls)) {
            findMethodsImpl(iface, name, paramCount, visitedClasses, methods);
        }
    }

    public GenericMethod findSingleAbstractMethod(GenericClass cls) {
        Map<MethodSignature, GenericMethod> methods = new HashMap<>();
        int count = findSingleAbstractMethodImpl(cls, new HashSet<>(), methods);
        if (count != 1) {
            return null;
        }
        for (GenericMethod method : methods.values()) {
            if (method.getDescriber().isAbstract()) {
                return method;
            }
        }
        return null;
    }

    private int findSingleAbstractMethodImpl(GenericClass cls, Set<String> visitedClasses,
            Map<MethodSignature, GenericMethod> methods) {
        if (cls.getName().equals(Object.class.getName())) {
            return 0;
        }
        if (!visitedClasses.add(cls.getName())) {
            return 0;
        }

        ClassDescriber describer = classRepository.describe(cls.getName());
        if (describer == null) {
            return 0;
        }

        Map<TypeVar, TypeArgument> substitutions = prepareSubstitutions(describer, cls);
        if (substitutions == null) {
            return 0;
        }

        int result = 0;
        ClassDescriber objectDescriber = classRepository.describe(Object.class.getName());
        for (MethodDescriber methodDesc : describer.getMethods()) {
            if (objectDescriber.getMethod(methodDesc.getName(), methodDesc.getParameterTypes()) != null) {
                continue;
            }
            if (!methodDesc.isAbstract() || methodDesc.isStatic()) {
                continue;
            }

            ValueType[] paramTypes = methodDesc.getParameterTypes();
            for (int i = 0; i < paramTypes.length; ++i) {
                if (paramTypes[i] instanceof GenericType) {
                    paramTypes[i] = ((GenericType) paramTypes[i]).substituteArgs(substitutions::get);
                }
            }

            ValueType returnType = methodDesc.getReturnType();
            if (returnType instanceof GenericType) {
                returnType = ((GenericType) returnType).substituteArgs(substitutions::get);
            }

            MethodSignature signature = new MethodSignature(methodDesc.getRawParameterTypes());
            if (!methods.containsKey(signature)) {
                methods.put(signature, new GenericMethod(methodDesc, cls, paramTypes, returnType));
                if (methodDesc.isAbstract()) {
                    ++result;
                    if (result > 1) {
                        break;
                    }
                }
            }
        }

        GenericClass supertype = getParent(cls);
        if (supertype != null && result <= 1) {
            result += findSingleAbstractMethodImpl(supertype, visitedClasses, methods);
        }
        for (GenericClass iface : getInterfaces(cls)) {
            if (result > 1) {
                break;
            }
            result += findSingleAbstractMethodImpl(iface, visitedClasses, methods);
        }

        return result;
    }

    static class MethodSignature {
        ValueType[] paramTypes;

        MethodSignature(ValueType[] paramTypes) {
            this.paramTypes = paramTypes;
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(paramTypes);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (!(obj instanceof MethodSignature)) {
                return false;
            }
            MethodSignature other = (MethodSignature) obj;
            return Arrays.equals(paramTypes, other.paramTypes);
        }
    }
}
