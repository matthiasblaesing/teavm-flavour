package org.teavm.flavour.templates.test;

import java.util.function.Supplier;
import org.teavm.flavour.templates.BindAttributeComponent;
import org.teavm.flavour.templates.BindContent;
import org.teavm.flavour.templates.ModifierTarget;
import org.teavm.flavour.templates.Renderable;
import org.teavm.jso.dom.html.HTMLElement;

@BindAttributeComponent(name = "attribute-value-copy")
public class AttributeValueCopyComponent implements Renderable {
    private HTMLElement element;
    private Supplier<String> value;

    public AttributeValueCopyComponent(ModifierTarget target) {
        this.element = target.getElement();
    }

    @BindContent
    public void setValue(Supplier<String> value) {
        this.value = value;
    }

    @Override
    public void render() {
        String text = value.get();
        element.setAttribute("class", text != null ? text : "");
    }

    @Override
    public void destroy() {
    }
}
