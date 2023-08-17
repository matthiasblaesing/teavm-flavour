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

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class ValueTypeFormatter {
    private Map<TypeVar, String> typeVarNames = new HashMap<>();
    private boolean usingShortClassNames;
    private boolean usingWildcardChars;

    public boolean isUsingShortClassNames() {
        return usingShortClassNames;
    }

    public void setUsingShortClassNames(boolean usingShortClassNames) {
        this.usingShortClassNames = usingShortClassNames;
    }

    public boolean isUsingWildcardChars() {
        return usingWildcardChars;
    }

    public void setUsingWildcardChars(boolean usingWildcardChars) {
        this.usingWildcardChars = usingWildcardChars;
    }

    public String format(ValueType type) {
        StringBuilder sb = new StringBuilder();
        format(type, sb);
        return sb.toString();
    }

    public void format(ValueType type, StringBuilder sb) {
        if (type == null) {
            sb.append('?');
            return;
        }
        if (type instanceof Primitive) {
            switch (((Primitive) type).getKind()) {
                case BOOLEAN:
                    sb.append("boolean");
                    break;
                case CHAR:
                    sb.append("char");
                    break;
                case BYTE:
                    sb.append("byte");
                    break;
                case SHORT:
                    sb.append("short");
                    break;
                case INT:
                    sb.append("int");
                    break;
                case LONG:
                    sb.append("long");
                    break;
                case FLOAT:
                    sb.append("float");
                    break;
                case DOUBLE:
                    sb.append("double");
                    break;
                default:
                    throw new AssertionError("Unexpected primitive type");
            }
        } else if (type instanceof GenericClass) {
            GenericClass cls = (GenericClass) type;
            if (!usingShortClassNames) {
                sb.append(cls.getName());
            } else {
                int index = cls.getName().lastIndexOf('.');
                sb.append(cls.getName().substring(index + 1));
            }
            if (!cls.getArguments().isEmpty()) {
                sb.append('<');
                for (int i = 0; i < cls.getArguments().size(); ++i) {
                    if (i > 0) {
                        sb.append(", ");
                    }
                    format(cls.getArguments().get(i), sb);
                }
                sb.append('>');
            }
        } else if (type instanceof GenericArray) {
            GenericArray array = (GenericArray) type;
            format(array.getElementType(), sb);
            sb.append("[]");
        } else if (type instanceof PrimitiveArray) {
            PrimitiveArray array = (PrimitiveArray) type;
            format(array.getElementType(), sb);
            sb.append("[]");
        } else if (type instanceof GenericReference) {
            GenericReference ref = (GenericReference) type;
            sb.append(getNameOfTypeVar(ref.getVar()));
        } else if (type instanceof NullType) {
            sb.append("-null-");
        } else if (type instanceof IntersectionType) {
            sb.append(((IntersectionType) type).getTypes().stream()
                    .map(t -> {
                        StringBuilder innerSb = new StringBuilder();
                        format(t, innerSb);
                        return innerSb.toString();
                    })
                    .sorted()
                    .collect(Collectors.joining(" & ")));
        } else {
            throw new AssertionError("Unexpected type: " + type.getClass().getName());
        }
    }

    public void format(TypeArgument type, StringBuilder sb) {
        switch (type.getVariance()) {
            case INVARIANT:
                format(type.getBound(), sb);
                break;
            case COVARIANT:
                if (type.getBound() instanceof GenericClass
                        && ((GenericClass) type.getBound()).getName().equals("java.lang.Object")) {
                    sb.append("?");
                } else {
                    sb.append("? extends ");
                    format(type.getBound(), sb);
                }
                break;
            case CONTRAVARIANT:
                sb.append("? super ");
                format(type.getBound(), sb);
                break;
        }
    }

    private String getNameOfTypeVar(TypeVar var) {
        if (var.getName() != null) {
            return var.getName();
        }
        if (usingWildcardChars) {
            return "?";
        }
        return typeVarNames.computeIfAbsent(var, k -> generateName(typeVarNames.size()));
    }

    private String generateName(int index) {
        int letterIndex = index % 26;
        int suffix = index / 26;
        StringBuilder sb = new StringBuilder("'");
        sb.append((char) ('a' + letterIndex));
        if (suffix > 0) {
            sb.append(suffix);
        }
        return sb.toString();
    }
}
