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
package org.teavm.flavour.regex.ast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 *
 * @author Alexey Andreev
 */
public class OneOfNode extends Node {
    private List<Node> elements = new ArrayList<>();

    public OneOfNode(Node... nodes) {
        elements.addAll(Arrays.asList(nodes));
    }

    public List<Node> getElements() {
        return elements;
    }

    @Override
    public void acceptVisitor(NodeVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public String toString() {
        return elements.stream().map(e -> "(" + e + ")").collect(Collectors.joining(" | "));
    }
}
