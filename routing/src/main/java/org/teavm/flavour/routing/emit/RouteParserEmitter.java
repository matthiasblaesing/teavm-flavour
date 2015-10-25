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
package org.teavm.flavour.routing.emit;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import org.teavm.dependency.DependencyAgent;
import org.teavm.diagnostics.Diagnostics;
import org.teavm.flavour.routing.PathSet;
import org.teavm.flavour.routing.Route;
import org.teavm.flavour.routing.metadata.ParameterDescriptor;
import org.teavm.flavour.routing.metadata.RouteDescriber;
import org.teavm.flavour.routing.metadata.RouteDescriptor;
import org.teavm.flavour.routing.metadata.RouteSetDescriptor;
import org.teavm.flavour.routing.parsing.PathBuilder;
import org.teavm.flavour.routing.parsing.PathParser;
import org.teavm.flavour.routing.parsing.PathParserBuilder;
import org.teavm.flavour.routing.parsing.PathParserEmitter;
import org.teavm.flavour.routing.parsing.PathParserResult;
import org.teavm.jso.browser.Window;
import org.teavm.model.AccessLevel;
import org.teavm.model.CallLocation;
import org.teavm.model.ClassHolder;
import org.teavm.model.ClassReader;
import org.teavm.model.ClassReaderSource;
import org.teavm.model.FieldHolder;
import org.teavm.model.MethodDescriptor;
import org.teavm.model.MethodHolder;
import org.teavm.model.MethodReference;
import org.teavm.model.Program;
import org.teavm.model.ValueType;
import org.teavm.model.emit.ChooseEmitter;
import org.teavm.model.emit.ProgramEmitter;
import org.teavm.model.emit.StringChooseEmitter;
import org.teavm.model.emit.ValueEmitter;

/**
 *
 * @author Alexey Andreev
 */
class RouteParserEmitter {
    public static final String PATH_READER_CLASS = Route.class.getPackage().getName() + ".PathReader";
    public static final String ROUTING_CLASS = Route.class.getPackage().getName() + ".Routing";
    private DependencyAgent agent;
    private ClassReaderSource classSource;
    private Diagnostics diagnostics;
    private PathParserEmitter pathParserEmitter;
    private RouteDescriber describer;
    private Map<String, String> routeReaders = new HashMap<>();
    private Map<String, String> routeIfaceReaders = new HashMap<>();
    int suffixGenerator;

    public RouteParserEmitter(DependencyAgent agent) {
        this.agent = agent;
        classSource = agent.getClassSource();
        diagnostics = agent.getDiagnostics();
        pathParserEmitter = new PathParserEmitter(agent);
    }


    public String getReader(String routeType) {
        return routeReaders.get(routeType);
    }

    public Program emitGetter(MethodReference method) {
        ProgramEmitter pe = ProgramEmitter.create(method.getDescriptor(), classSource);
        ValueEmitter typeVar = pe.var(1, String.class);

        StringChooseEmitter choice = pe.stringChoice(typeVar);
        for (String routeImpl : routeReaders.keySet()) {
            String readerType = routeReaders.get(routeImpl);
            choice.option(routeImpl, () -> {
                pe.construct(readerType).returnValue();
            });
        }
        choice.otherwise(() -> pe.constantNull(ValueType.object(PATH_READER_CLASS)).returnValue());

        return pe.getProgram();
    }

    public String emitParser(String className, CallLocation location) {
        return routeReaders.computeIfAbsent(className, implType -> {
            Set<ClassReader> pathSets = new HashSet<>();
            getPathSets(implType, pathSets, new HashSet<>());
            if (pathSets.isEmpty()) {
                diagnostics.error(location, "Given handler {{c0}} does not implement path set", implType);
                return null;
            } else if (pathSets.size() > 1) {
                Iterator<ClassReader> iter = pathSets.iterator();
                ClassReader example1 = iter.next();
                ClassReader example2 = iter.next();
                diagnostics.error(location, "Given handler {{c0}} implements several path sets. Examples are {{c1}} "
                        + "and {{c2}}", implType, example1, example2);
                return null;
            }
            ClassReader routeType = pathSets.iterator().next();

            return routeIfaceReaders.computeIfAbsent(routeType.getName(), ifaceType -> {
                return emitInterfaceParser(ifaceType, location);
            });
        });
    }

    private String emitInterfaceParser(String routeIface, CallLocation location) {
        describer = new RouteDescriber(classSource, diagnostics, location);
        RouteSetDescriptor descriptor = describer.describeRouteSet(routeIface);
        if (descriptor == null) {
            return null;
        }

        ClassHolder readerClass = new ClassHolder(PATH_READER_CLASS + suffixGenerator++);
        readerClass.setLevel(AccessLevel.PACKAGE_PRIVATE);
        readerClass.setParent("java.lang.Object");
        readerClass.getInterfaces().add(PATH_READER_CLASS);

        FieldHolder parserField = new FieldHolder("pathParser");
        parserField.setLevel(AccessLevel.PRIVATE);
        parserField.setType(ValueType.parse(PathParser.class));
        readerClass.addField(parserField);

        MethodHolder ctor = new MethodHolder("<init>", ValueType.VOID);
        ctor.setLevel(AccessLevel.PUBLIC);
        emitConstructor(ProgramEmitter.create(ctor, classSource), readerClass.getName(), descriptor);
        readerClass.addMethod(ctor);

        MethodHolder worker = new MethodHolder("read", ValueType.parse(String.class), ValueType.parse(Route.class),
                ValueType.BOOLEAN);
        worker.setLevel(AccessLevel.PUBLIC);
        emitWorker(ProgramEmitter.create(worker, classSource), readerClass.getName(), routeIface, descriptor);
        readerClass.addMethod(worker);

        agent.submitClass(readerClass);

        return readerClass.getName();
    }

    private void emitConstructor(ProgramEmitter pe, String className, RouteSetDescriptor descriptor) {
        PathParser pathParser = createPathParser(descriptor);
        ValueEmitter thisVar = pe.var(0, ValueType.object(className));
        thisVar.invokeSpecial(Object.class, "<init>");
        thisVar.setField("pathParser", pathParserEmitter.emitWorker(pe, pathParser));
        pe.exit();
    }

    private void emitWorker(ProgramEmitter pe, String className, String routeType, RouteSetDescriptor descriptor) {
        ValueEmitter thisVar = pe.var(0, ValueType.object(className));
        ValueEmitter pathVar = pe.var(1, String.class);
        ValueEmitter routeVar = pe.var(2, Route.class).cast(ValueType.object(routeType));
        ValueEmitter parserVar = thisVar.getField("pathParser", PathParser.class);

        ValueEmitter parseResultVar = parserVar.invokeVirtual("parse", PathParserResult.class, pathVar);
        ValueEmitter caseVar = parseResultVar.invokeVirtual("getCaseIndex", int.class);
        pe.when(caseVar.isLessThan(pe.constant(0)))
                .thenDo(() -> pe.constant(0).returnValue());

        ChooseEmitter choice = pe.choice(caseVar);
        for (int i = 0; i < descriptor.getRoutes().size(); ++i) {
            RouteDescriptor routeDescriptor = descriptor.getRoutes().get(i);
            MethodDescriptor method = routeDescriptor.getMethod();
            ValueEmitter[] values = new ValueEmitter[method.parameterCount()];
            choice.option(i, () -> {
                for (int j = 0; j < routeDescriptor.pathPartCount() - 1; ++j) {
                    ParameterDescriptor param = routeDescriptor.parameter(j);
                    ValueEmitter startVar = parseResultVar.invokeVirtual("start", int.class, pe.constant(j));
                    ValueEmitter endVar = parseResultVar.invokeVirtual("end", int.class, pe.constant(j));
                    ValueEmitter rawParamVar = pathVar.invokeVirtual("substring", String.class, startVar, endVar);
                    values[param.getJavaIndex()] = emitParam(rawParamVar, param);
                }
                routeVar.invokeVirtual(method.getName(), method.getResultType(), values);
            });
        }
        choice.otherwise(() -> pe.constant(0).returnValue());

        pe.constant(1).returnValue();
    }

    private ValueEmitter emitParam(ValueEmitter stringVar, ParameterDescriptor param) {
        ProgramEmitter pe = stringVar.getProgramEmitter();
        stringVar = pe.invoke(Window.class, "decodeURIComponent", String.class, stringVar);
        switch (param.getType()) {
            case STRING:
                return stringVar;
            case BYTE:
                return pe.invoke(Byte.class, "parseByte", byte.class, stringVar);
            case SHORT:
                return pe.invoke(Short.class, "parseShort", short.class, stringVar);
            case INTEGER:
                return pe.invoke(Integer.class, "parseInt", int.class, stringVar);
            case LONG:
                return pe.invoke(Long.class, "parseLong", long.class, stringVar);
            case FLOAT:
                return pe.invoke(Float.class, "parseFloat", float.class, stringVar);
            case DOUBLE:
                return pe.invoke(Double.class, "parseDouble", double.class, stringVar);
            case DATE: {
                ValueEmitter millis = pe.invoke(ROUTING_CLASS, "parseDate", ValueType.LONG, stringVar);
                return pe.construct(Date.class, millis);
            }
            case ENUM: {
                String className = ((ValueType.Object) param.getValueType()).getClassName();
                return pe.invoke(className, "valueOf", param.getValueType(), stringVar);
            }
            case BIG_DECIMAL:
                return pe.construct(BigDecimal.class, stringVar);
            case BIG_INTEGER:
                return pe.construct(BigInteger.class, stringVar);
            default:
                throw new AssertionError("Unknown type: " + param.getType());
        }
    }

    private PathParser createPathParser(RouteSetDescriptor descriptor) {
        PathParserBuilder pathParserBuilder = new PathParserBuilder();
        for (RouteDescriptor routeDescriptor : descriptor.getRoutes()) {
            PathBuilder pathBuilder = pathParserBuilder.path();
            pathBuilder.text(routeDescriptor.pathPart(0));
            for (int i = 1; i < routeDescriptor.pathPartCount(); ++i) {
                ParameterDescriptor param = routeDescriptor.parameter(i - 1);
                pathBuilder.escapedRegex(param.getEffectivePattern());
                pathBuilder.text(routeDescriptor.pathPart(i));
            }
        }
        return pathParserBuilder.buildUnprepared();
    }

    private void getPathSets(String className, Set<ClassReader> pathSets, Set<String> visited) {
        if (!visited.add(className)) {
            return;
        }

        ClassReader cls = classSource.get(className);
        if (cls == null) {
            return;
        }

        if (cls.getAnnotations().get(PathSet.class.getName()) != null) {
            pathSets.add(cls);
            return;
        }

        if (cls.getParent() != null && !cls.getParent().equals(cls.getName())) {
            getPathSets(cls.getParent(), pathSets, visited);
        }
        for (String iface : cls.getInterfaces()) {
            getPathSets(iface, pathSets, visited);
        }
    }
}
