package org.sitenetsoft.sunseterp.framework.base.util;

import java.util.Map;

/**
 * Contains a simple script (scriptlet).
 * <p>A scriptlet is a small script that is commonly found in a scripting XML file.
 * The scriptlet is composed of two parts: the prefix - which is the script language
 * followed by a colon (":"), and the script. Example: <code>groovy:return foo.bar();</code>.
 *
 */
public final class Scriptlet {

    private final String language;
    private final String script;

    public Scriptlet(String script) {
        Assert.notNull("script", script);
        int colonPos = script.indexOf(":");
        if (colonPos == -1) {
            throw new IllegalArgumentException("Invalid script: " + script);
        }
        this.language = script.substring(0, colonPos);
        this.script = script.substring(colonPos + 1);
    }

    /**
     * Executes the scriptlet and returns the result.
     * @param context The script bindings
     * @return The scriptlet result
     * @throws Exception
     */
    public Object executeScript(Map<String, Object> context) throws Exception {
        return ScriptUtil.evaluate(language, script, null, context);
    }

    @Override
    public String toString() {
        return this.language.concat(":").concat(this.script);
    }
}
