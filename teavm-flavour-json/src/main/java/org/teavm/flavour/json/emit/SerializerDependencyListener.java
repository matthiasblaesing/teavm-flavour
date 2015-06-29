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
package org.teavm.flavour.json.emit;

import org.teavm.dependency.AbstractDependencyListener;
import org.teavm.dependency.DependencyAgent;
import org.teavm.dependency.DependencyConsumer;
import org.teavm.dependency.DependencyType;
import org.teavm.dependency.MethodDependency;
import org.teavm.flavour.json.JSON;
import org.teavm.flavour.json.serializer.JsonSerializerContext;
import org.teavm.flavour.json.tree.Node;
import org.teavm.model.CallLocation;
import org.teavm.model.MethodReference;
import org.teavm.model.ValueType;

/**
 *
 * @author Alexey Andreev
 */
class SerializerDependencyListener extends AbstractDependencyListener {
    private JsonSerializerEmitter emitter;

    public String getSerializer(String className) {
        if (emitter == null) {
            return null;
        }
        return emitter.getClassSerializer(className);
    }

    @Override
    public void methodReached(final DependencyAgent agent, final MethodDependency method,
            final CallLocation location) {
        if (method.getReference().getClassName().equals(JSON.class.getName()) &&
                method.getReference().getName().equals("getClassSerializer")) {
            emitter = new JsonSerializerEmitter(agent);
            MethodDependency serializeMethod = agent.linkMethod(new MethodReference(JSON.class,
                    "serialize", JsonSerializerContext.class, Object.class, Node.class), null);
            serializeMethod.getVariable(2).addConsumer(new DependencyConsumer() {
                @Override
                public void consume(DependencyType type) {
                    String serializer = emitter.addClassSerializer(type.getName());
                    agent.linkClass(serializer, location);
                    agent.linkMethod(new MethodReference(serializer, "<init>", ValueType.VOID), location)
                            .propagate(0, agent.getType(serializer))
                            .use();
                    method.getResult().propagate(agent.getType(serializer));
                }
            });
        }
    }
}
