/*
 *  Copyright 2016 Alexey Andreev.
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

import java.util.Map;

public class MapSubstitutions implements Substitutions {
    private Map<TypeVar, GenericType> map;

    public MapSubstitutions(Map<TypeVar, GenericType> map) {
        this.map = map;
    }

    public Map<TypeVar, GenericType> getMap() {
        return map;
    }

    @Override
    public GenericType get(TypeVar var) {
        return map.get(var);
    }
}
