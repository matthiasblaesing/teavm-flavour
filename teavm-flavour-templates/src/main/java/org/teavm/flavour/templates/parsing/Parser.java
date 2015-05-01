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
package org.teavm.flavour.templates.parsing;

import java.io.*;
import java.util.*;
import net.htmlparser.jericho.*;
import org.apache.commons.lang3.StringUtils;
import org.teavm.flavour.expr.*;
import org.teavm.flavour.expr.Compiler;
import org.teavm.flavour.expr.ast.Expr;
import org.teavm.flavour.expr.type.GenericClass;
import org.teavm.flavour.expr.type.ValueType;
import org.teavm.flavour.expr.type.meta.ClassDescriber;
import org.teavm.flavour.expr.type.meta.ClassDescriberRepository;
import org.teavm.flavour.expr.type.meta.MethodDescriber;
import org.teavm.flavour.templates.tree.*;

/**
 *
 * @author Alexey Andreev
 */
public class Parser {
    private ClassDescriberRepository classRepository;
    private ImportingClassResolver classResolver;
    private ResourceProvider resourceProvider;
    private Map<String, DirectiveMetadata> directives = new HashMap<>();
    private List<Diagnostic> diagnostics = new ArrayList<>();
    private Map<String, Deque<ValueType>> variables = new HashMap<>();
    private Source source;
    private int position;

    public Parser(ClassDescriberRepository classRepository, ClassResolver classResolver,
            ResourceProvider resourceProvider) {
        this.classRepository = classRepository;
        this.classResolver = new ImportingClassResolver(classResolver);
        this.resourceProvider = resourceProvider;
    }

    public List<Diagnostic> getDiagnostics() {
        return diagnostics;
    }

    public boolean wasSuccessful() {
        return diagnostics.isEmpty();
    }

    public List<TemplateNode> parse(Reader reader, String className) throws IOException {
        source = new Source(reader);
        use(source, "std", "org.teavm.flavour.templates.directives");
        pushVar("this", new GenericClass(className));
        position = source.getBegin();
        List<TemplateNode> nodes = new ArrayList<>();
        parseSegment(source.getEnd(), nodes);
        popVar("this");
        source = null;
        return nodes;
    }

    private void parseSegment(int limit, List<TemplateNode> result) {
        while (position < limit) {
            Tag tag = source.getNextTag(position);
            if (tag == null || tag.getBegin() > limit) {
                if (position < limit) {
                    result.add(new DOMText(source.subSequence(position, limit).toString()));
                }
                position = limit;
                break;
            }

            if (position < tag.getBegin()) {
                result.add(new DOMText(source.subSequence(position, tag.getBegin()).toString()));
            }
            position = tag.getEnd();
            parseTag(tag, result);
        }
    }

    private void parseTag(Tag tag, List<TemplateNode> result) {
        if (tag instanceof StartTag) {
            StartTag startTag = (StartTag)tag;
            if (startTag.getStartTagType() == StartTagType.XML_PROCESSING_INSTRUCTION) {
                parseProcessingInstruction(startTag);
            } else if (startTag.getStartTagType() == StartTagType.NORMAL) {
                result.add(parseElement(tag.getElement()));
            }
        }
    }

    private TemplateNode parseElement(Element elem) {
        if (elem.getName().indexOf(':') > 0) {
            return parseDirective(elem);
        } else {
            return parseDomElement(elem);
        }
    }

    private TemplateNode parseDomElement(Element elem) {
        DOMElement templateElem = new DOMElement(elem.getName());
        for (int i = 0; i < elem.getAttributes().size(); ++i) {
            Attribute attr = elem.getAttributes().get(i);
            templateElem.setAttribute(attr.getName(), attr.getValue());
        }
        parseSegment(elem.getEnd(), templateElem.getChildNodes());
        return templateElem;
    }

    private TemplateNode parseDirective(Element elem) {
        int prefixLength = elem.getName().indexOf(':');
        String prefix = elem.getName().substring(0, prefixLength);
        String name = elem.getName().substring(prefixLength + 1);
        String fullName = prefix + ":" + name;
        DirectiveMetadata directiveMeta = directives.get(fullName);
        if (directiveMeta == null) {
            error(elem.getStartTag().getNameSegment(), "Undefined directive " + fullName);
            return null;
        }

        DirectiveBinding directive = new DirectiveBinding(directiveMeta.cls.getName());

        Map<String, ValueType> declaredVars = new HashMap<>();
        for (DirectiveAttributeMetadata attrMeta : directiveMeta.attributes.values()) {
            Attribute attr = elem.getAttributes().get(attrMeta.name);
            if (attr == null) {
                if (attrMeta.required) {
                    error(elem.getStartTag(), "Missing required attribute: " + attrMeta.name);
                }
                continue;
            }
            MethodDescriber setter = attrMeta.setter;
            switch (attrMeta.type) {
                case VARIABLE: {
                    String varName = attr.getValue();
                    if (declaredVars.containsKey(varName)) {
                        error(attr.getValueSegment(), "Variable " + varName + " is already used by the same " +
                                "directive");
                    } else {
                        declaredVars.put(varName, attrMeta.valueType);
                    }
                    DirectiveVariableBinding varBinding = new DirectiveVariableBinding(
                            setter.getOwner().getName(), setter.getName(), varName, attrMeta.valueType);
                    directive.getVariables().add(varBinding);
                    break;
                }
                case COMPUTATION: {
                    TypedPlan plan = compileExpr(attr.getValueSegment(), attrMeta.valueType);
                    DirectiveComputationBinding computationBinding = new DirectiveComputationBinding(
                            setter.getOwner().getName(), setter.getName(), plan);
                    directive.getComputations().add(computationBinding);
                    break;
                }
                case ACTION: {
                    TypedPlan plan = compileExpr(attr.getValueSegment(), null);
                    DirectiveActionBinding actionBinding = new DirectiveActionBinding(
                            setter.getOwner().getName(), setter.getName(), plan.getPlan());
                    directive.getActions().add(actionBinding);
                    break;
                }
            }
        }

        for (Map.Entry<String, ValueType> varEntry : declaredVars.entrySet()) {
            pushVar(varEntry.getKey(), varEntry.getValue());
        }

        Segment content = elem.getContent();
        if (directiveMeta.contentSetter != null) {
            parseSegment(elem.getEnd(), directive.getContentNodes());
            directive.setContentMethodName(directiveMeta.contentSetter.getName());
        } else if (!directiveMeta.ignoreContent) {
            if (content.getNodeIterator().hasNext()) {
                error(elem, "Directive " + directiveMeta.cls.getName() + " should not have any content");
            }
        }

        for (String varName : declaredVars.keySet()) {
            popVar(varName);
        }

        return directive;
    }

    private TypedPlan compileExpr(Segment segment, ValueType type) {
        org.teavm.flavour.expr.Parser exprParser = new org.teavm.flavour.expr.Parser(classResolver);
        Expr<Void> expr = exprParser.parse(segment.toString());
        int offset = segment.getBegin();
        for (Diagnostic diagnostic : exprParser.getDiagnostics()) {
            diagnostic = new Diagnostic(offset + diagnostic.getStart(), offset + diagnostic.getEnd(),
                    diagnostic.getMessage());
            diagnostics.add(diagnostic);
        }
        Compiler compiler = new Compiler(classRepository, classResolver, new TemplateScope());
        TypedPlan result = compiler.compile(expr, type);
        for (Diagnostic diagnostic : exprParser.getDiagnostics()) {
            diagnostic = new Diagnostic(offset + diagnostic.getStart(), offset + diagnostic.getEnd(),
                    diagnostic.getMessage());
            diagnostics.add(diagnostic);
        }
        return result;
    }

    private void pushVar(String name, ValueType type) {
        Deque<ValueType> stack = variables.get(name);
        if (stack == null) {
            stack = new ArrayDeque<>();
            variables.put(name, stack);
        }
        stack.push(type);
    }

    private void popVar(String name) {
        Deque<ValueType> stack = variables.get(name);
        if (stack != null) {
            stack.pop();
            if (stack.isEmpty()) {
                variables.remove(stack);
            }
        }
    }

    class TemplateScope implements Scope {
        @Override
        public ValueType variableType(String variableName) {
            Deque<ValueType> stack = variables.get(variableName);
            return stack != null && !stack.isEmpty() ? stack.peek() : null;
        }
    }

    private void parseProcessingInstruction(StartTag tag) {
        if (tag.getName().equals("?import")) {
            parseImport(tag);
        } else if (tag.getName().equals("?use")) {
            parseUse(tag);
        }
    }

    private void parseImport(StartTag tag) {
        String importedName = normalizeQualifiedName(tag.getTagContent().toString());
        if (importedName.endsWith(".*")) {
            classResolver.importPackage(importedName);
        } else {
            if (classResolver.findClass(importedName) == null) {
                error(tag.getTagContent(), "Class was not found: " + importedName);
            } else {
                classResolver.importClass(importedName);
            }
        }
    }

    private void parseUse(StartTag tag) {
        String content = tag.getTagContent().toString();
        String[] parts = StringUtils.split(content, ":", 2);
        if (parts.length != 2) {
            error(tag.getTagContent(), "Illegal syntax for 'use' instruction");
            return;
        }

        String prefix = parts[0].trim();
        String packageName = normalizeQualifiedName(parts[1]);
        use(tag.getTagContent(), prefix, packageName);
    }

    private void use(Segment segment, String prefix, String packageName) {
        String resourceName = "META-INF/flavour/directive-packages/" + packageName;
        try (InputStream input = resourceProvider.openResource(resourceName)) {
            if (input == null) {
                error(segment, "Directive package was not found: " + packageName);
                return;
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(input));
            while (true) {
                String line = reader.readLine();
                if (line == null) {
                    break;
                }
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                String className = packageName + "." + line;

                ClassDescriber cls = classRepository.describe(className);
                if (cls == null) {
                    error(segment, "Class " + className + " declared by directive package was not found");
                    continue;
                }

                DirectiveParser directiveParser = new DirectiveParser(classRepository, diagnostics, segment);
                DirectiveMetadata directiveMeta = directiveParser.parse(cls);
                if (directiveMeta != null) {
                    directives.put(prefix + ":" + directiveMeta.name, directiveMeta);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("IO exception occured parsing HTML input", e);
        }
    }

    private String normalizeQualifiedName(String text) {
        String[] parts = StringUtils.split(text.trim(), '.');
        for (int i = 0; i < parts.length; ++i) {
            parts[i] = parts[i].trim();
        }
        return StringUtils.join(parts, '.');
    }

    private void error(Segment segment, String message) {
        diagnostics.add(new Diagnostic(segment.getBegin(), segment.getEnd(), message));
    }
}
