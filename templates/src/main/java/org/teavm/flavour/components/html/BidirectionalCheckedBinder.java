/*
 *  Copyright 2017 Alexey Andreev.
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
package org.teavm.flavour.components.html;

import java.util.function.Supplier;
import org.teavm.flavour.templates.BindAttributeComponent;
import org.teavm.flavour.templates.BindContent;
import org.teavm.flavour.templates.ModifierTarget;
import org.teavm.flavour.templates.Renderable;
import org.teavm.flavour.templates.ValueChangeListener;
import org.teavm.jso.dom.events.Event;
import org.teavm.jso.dom.events.EventListener;
import org.teavm.jso.dom.html.HTMLInputElement;

@BindAttributeComponent(name = "bidir-checked")
public class BidirectionalCheckedBinder implements Renderable {
    private HTMLInputElement element;
    private Supplier<Boolean> value;
    private boolean cachedValue;
    private ValueChangeListener<Boolean> listener;
    private boolean bound;

    public BidirectionalCheckedBinder(ModifierTarget target) {
        this.element = (HTMLInputElement) target.getElement();
    }

    @BindContent
    public void setValue(Supplier<Boolean> value) {
        this.value = value;
    }

    @BindContent
    public void setListener(ValueChangeListener<Boolean> listener) {
        this.listener = listener;
    }

    @Override
    public void render() {
        try {
            boolean newValue = value.get();
            if (newValue != cachedValue) {
                cachedValue = newValue;
                element.setChecked(newValue);
            }

            if (!bound) {
                bound = true;
                element.addEventListener("change", nativeListener);
            }
        } catch (Exception xpt) {
            System.out.println("BidirectionalCheckedBinder: Exception in render(): " + xpt.getMessage());
        }
    }

    @Override
    public void destroy() {
        if (bound) {
            bound = false;
            element.removeEventListener("change", nativeListener);
        }
    }

    private EventListener nativeListener = new EventListener() {
        @Override
        public void handleEvent(Event evt) {
            listener.changed(element.isChecked());
        }
    };
}
