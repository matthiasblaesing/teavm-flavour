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
package org.teavm.flavour.json.deserializer;

import java.util.HashMap;
import java.util.Map;

public class JsonDeserializerContext {
    private Map<Object, Object> objects = new HashMap<>();

    public void register(Object id, Object object) {
        if (objects.containsKey(id)) {
            throw new IllegalArgumentException("Two objects with the same id were found");
        }
        objects.put(id, object);
    }

    public Object get(Object id) {
        Object object = objects.get(id);
        if (object == null) {
            throw new IllegalArgumentException("Object with id " + id + " was not found");
        }
        return object;
    }
}
