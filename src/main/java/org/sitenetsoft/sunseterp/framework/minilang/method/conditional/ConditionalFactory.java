package org.sitenetsoft.sunseterp.framework.minilang.method.conditional;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.ServiceLoader;

import org.sitenetsoft.sunseterp.framework.base.util.Debug;
import org.sitenetsoft.sunseterp.framework.base.util.UtilGenerics;
import org.sitenetsoft.sunseterp.framework.minilang.MiniLangException;
import org.sitenetsoft.sunseterp.framework.minilang.SimpleMethod;
import org.w3c.dom.Element;

/**
 * An abstract factory class for creating &lt;if&gt; element sub-element implementations.
 * <p>Mini-language can be extended to support additional condition elements
 * by extending this class to provide custom conditional element implementations.
 *
 */
public abstract class ConditionalFactory<C extends Conditional> {

    private static final String MODULE = ConditionalFactory.class.getName();
    private static final Map<String, ConditionalFactory<?>> CONDITIONAL_FACTORIES;

    static {
        Map<String, ConditionalFactory<?>> factories = new HashMap<>();
        Iterator<ConditionalFactory<?>> it = UtilGenerics.cast(ServiceLoader.load(ConditionalFactory.class,
                ConditionalFactory.class.getClassLoader()).iterator());
        while (it.hasNext()) {
            ConditionalFactory<?> factory = it.next();
            factories.put(factory.getName(), factory);
        }
        CONDITIONAL_FACTORIES = Collections.unmodifiableMap(factories);
    }

    public static Conditional makeConditional(Element element, SimpleMethod simpleMethod) throws MiniLangException {
        String tagName = element.getTagName();
        ConditionalFactory<?> factory = CONDITIONAL_FACTORIES.get(tagName);
        if (factory != null) {
            return factory.createCondition(element, simpleMethod);
        } else {
            Debug.logWarning("Found an unknown if condition: " + tagName, MODULE);
            return null;
        }
    }

    public abstract C createCondition(Element element, SimpleMethod simpleMethod) throws MiniLangException;

    public abstract String getName();
}
