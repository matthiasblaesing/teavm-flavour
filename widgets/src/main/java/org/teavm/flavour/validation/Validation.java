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
package org.teavm.flavour.validation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class Validation<T> {
    Validator validator;
    boolean valid = true;
    boolean validFormat = true;
    Supplier<Converter<T>> converter;
    Supplier<T> supplier;
    Consumer<T> consumer;
    List<ValidationRule<T>> rules = new ArrayList<>();
    Set<BindValidation<T>> bindings = new HashSet<>();

    public boolean isValidFormat() {
        return validFormat;
    }

    public boolean isValid() {
        return valid && validFormat;
    }

    void check() {
        for (BindValidation<T> binding : bindings) {
            binding.check();
        }
    }
}
