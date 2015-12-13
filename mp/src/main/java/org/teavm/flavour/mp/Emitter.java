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
package org.teavm.flavour.mp;

/**
 *
 * @author Alexey Andreev
 */
public interface Emitter<T> {
    EmitterContext getContext();

    <S> Value<S> emit(Computation<S> computation);

    void emit(Action action);

    <S> Choice<S> choose(ReflectClass<S> type);

    <S> Choice<S> choose(Class<S> type);

    void returnValue(Computation<T> computation);
}
