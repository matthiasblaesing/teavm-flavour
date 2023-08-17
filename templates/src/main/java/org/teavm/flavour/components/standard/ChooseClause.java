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
package org.teavm.flavour.components.standard;

import java.util.function.BooleanSupplier;
import org.teavm.flavour.templates.BindAttribute;
import org.teavm.flavour.templates.BindContent;
import org.teavm.flavour.templates.Fragment;

public class ChooseClause {
    BooleanSupplier predicate;
    Fragment content;

    @BindAttribute(name = "when")
    public void setPredicate(BooleanSupplier predicate) {
        this.predicate = predicate;
    }

    @BindContent
    public void setContent(Fragment content) {
        this.content = content;
    }
}
