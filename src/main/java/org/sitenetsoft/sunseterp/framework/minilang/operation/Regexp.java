/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *******************************************************************************/
package org.sitenetsoft.sunseterp.framework.minilang.operation;

import org.sitenetsoft.sunseterp.framework.base.util.Debug;
import org.sitenetsoft.sunseterp.framework.base.util.GeneralException;
import org.sitenetsoft.sunseterp.framework.base.util.ObjectType;
import org.sitenetsoft.sunseterp.framework.base.util.PatternFactory;
import org.apache.oro.text.regex.MalformedPatternException;
import org.apache.oro.text.regex.Pattern;
import org.apache.oro.text.regex.PatternMatcher;
import org.apache.oro.text.regex.Perl5Matcher;
import org.w3c.dom.Element;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Validates the current field using a regular expression
 */
public class Regexp extends SimpleMapOperation {

    private static final String MODULE = Regexp.class.getName();
    private Pattern pattern = null;
    private String expr;

    public Regexp(Element element, SimpleMapProcess simpleMapProcess) {
        super(element, simpleMapProcess);
        expr = element.getAttribute("expr");
        try {
            pattern = PatternFactory.createOrGetPerl5CompiledPattern(expr, true);
        } catch (MalformedPatternException e) {
            Debug.logError(e, MODULE);
        }
    }

    @Override
    public void exec(Map<String, Object> inMap, Map<String, Object> results, List<Object> messages, Locale locale, ClassLoader loader) {
        Object obj = inMap.get(getFieldName());
        String fieldValue = null;
        try {
            fieldValue = (String) ObjectType.simpleTypeOrObjectConvert(obj, "String", null, locale);
        } catch (GeneralException e) {
            messages.add("Could not convert field value for comparison: " + e.getMessage());
            return;
        }
        if (pattern == null) {
            messages.add("Could not compile regular expression \"" + expr + "\" for validation");
            return;
        }
        PatternMatcher matcher = new Perl5Matcher();
        if (!matcher.matches(fieldValue, pattern)) {
            addMessage(messages, loader, locale);
        }
    }
}
