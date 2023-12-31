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

package org.sitenetsoft.sunseterp.framework.common;

import freemarker.template.TemplateException;
import org.apache.commons.io.FileUtils;
import org.sitenetsoft.sunseterp.framework.base.util.Debug;
import org.sitenetsoft.sunseterp.framework.base.util.FileUtil;
import org.sitenetsoft.sunseterp.framework.base.util.UtilMisc;
import org.sitenetsoft.sunseterp.framework.base.util.UtilProperties;
import org.sitenetsoft.sunseterp.framework.base.util.template.FreeMarkerWorker;
import org.sitenetsoft.sunseterp.framework.service.DispatchContext;
import org.sitenetsoft.sunseterp.framework.service.ServiceUtil;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.*;

// Use the createJsLanguageFileMapping service to create or update JsLanguageFilesMapping.java and JsLanguageFilesMapping.ftl files.
// You will still need to compile thereafter

public class JsLanguageFileMappingCreator {

    private static final String MODULE = JsLanguageFileMappingCreator.class.getName();

    public static Map<String, Object> createJsLanguageFileMapping(DispatchContext ctx, Map<String, ?> context) {
        Map<String, Object> result = ServiceUtil.returnSuccess();
        String encoding = (String) context.get("encoding"); // default value: UTF-8

        List<Locale> localeList = UtilMisc.availableLocales();
        Map<String, Object> jQueryLocaleFile = new LinkedHashMap<>();
        Map<String, String> dateJsLocaleFile = new LinkedHashMap<>();
        Map<String, String> validationLocaleFile = new LinkedHashMap<>();
        Map<String, String> dateTimePickerLocaleFile = new LinkedHashMap<>();
        Map<String, String> select2LocaleFile = new LinkedHashMap<>();

        // setup some variables to locate the js files
        String componentRoot = "component://common-theme/webapp";
        String jqueryUiLocaleRelPath = "/common/js/jquery/ui/i18n/";
        String dateJsLocaleRelPath = "/common/js/jquery/plugins/datejs/";
        String validateRelPath = "/common/js/node_modules/jquery-validation/dist/localization/";
        String dateTimePickerJsLocaleRelPath = "/common/js/jquery/plugins/datetimepicker/i18n/";
        String select2LocaleRelPath = "/common/js/jquery/plugins/select2/js/i18n/";
        String jsFilePostFix = ".js";
        String dateJsLocalePrefix = "date-";
        String validateLocalePrefix = "messages_";
        String jqueryUiLocalePrefix = "datepicker-";
        String dateTimePickerPrefix = "jquery-ui-timepicker-";
        String defaultLocaleDateJs = "en-US";
        String defaultLocaleJquery = "en"; // Beware to keep the OFBiz specific datepicker-en.js file when upgrading...

        for (Locale locale : localeList) {
            String displayCountry = locale.toString();
            String modifiedDisplayCountry = null;
            String modifiedDisplayCountryForValidation = null;
            if (displayCountry.contains("_")) {
                modifiedDisplayCountry = displayCountry.replace("_", "-");
                modifiedDisplayCountryForValidation = displayCountry; // messages*.js use "_" not "-" as others
            } else {
                modifiedDisplayCountry = displayCountry;
            }

            String strippedLocale = locale.getLanguage();

            File file = null;
            String fileUrl = null;

            /*
             * Try to open the date-js language file
             */
            String fileName = componentRoot + dateJsLocaleRelPath + dateJsLocalePrefix + modifiedDisplayCountry + jsFilePostFix;
            file = FileUtil.getFile(fileName);

            if (file.exists()) {
                fileUrl = dateJsLocaleRelPath + dateJsLocalePrefix + modifiedDisplayCountry + jsFilePostFix;
            } else {
                // Try to guess a language
                String tmpLocale = strippedLocale + "-" + strippedLocale.toUpperCase(Locale.getDefault());
                fileName = componentRoot + dateJsLocaleRelPath + dateJsLocalePrefix + tmpLocale + jsFilePostFix;
                file = FileUtil.getFile(fileName);
                if (file.exists()) {
                    fileUrl = dateJsLocaleRelPath + dateJsLocalePrefix + tmpLocale + jsFilePostFix;
                } else {
                    // use language en-US as fallback
                    fileUrl = dateJsLocaleRelPath + dateJsLocalePrefix + defaultLocaleDateJs + jsFilePostFix;
                }
            }
            dateJsLocaleFile.put(displayCountry, fileUrl);

            /*
             * Try to open the jquery validation language file
             */
            if (modifiedDisplayCountryForValidation != null) { // Try 1st lang_country
                fileName = componentRoot + validateRelPath + validateLocalePrefix + modifiedDisplayCountryForValidation + jsFilePostFix;
                file = FileUtil.getFile(fileName);
                if (file.exists()) {
                    fileUrl = validateRelPath + validateLocalePrefix + modifiedDisplayCountryForValidation + jsFilePostFix;
                } else { // lang only
                    fileName = componentRoot + validateRelPath + validateLocalePrefix + strippedLocale + jsFilePostFix;
                    file = FileUtil.getFile(fileName);
                    if (file.exists()) {
                        fileUrl = validateRelPath + validateLocalePrefix + strippedLocale + jsFilePostFix;
                    } else {
                        // use default language en as fallback
                        fileUrl = validateRelPath + validateLocalePrefix + defaultLocaleJquery + jsFilePostFix;
                    }
                }
            } else { // Then try lang only
                fileName = componentRoot + validateRelPath + validateLocalePrefix + strippedLocale + jsFilePostFix;
                file = FileUtil.getFile(fileName);
                if (file.exists()) {
                    fileUrl = validateRelPath + validateLocalePrefix + strippedLocale + jsFilePostFix;
                } else {
                    // use default language en as fallback
                    fileUrl = validateRelPath + validateLocalePrefix + defaultLocaleJquery + jsFilePostFix;
                }
            }
            validationLocaleFile.put(displayCountry, fileUrl);

            /*
             * Try to open the jquery timepicker language file
             */
            fileName = componentRoot + jqueryUiLocaleRelPath + jqueryUiLocalePrefix + strippedLocale + jsFilePostFix;
            file = FileUtil.getFile(fileName);

            if (file.exists()) {
                fileUrl = jqueryUiLocaleRelPath + jqueryUiLocalePrefix + strippedLocale + jsFilePostFix;
            } else {
                // Try to guess a language
                fileName = componentRoot + jqueryUiLocaleRelPath + jqueryUiLocalePrefix + modifiedDisplayCountry + jsFilePostFix;
                file = FileUtil.getFile(fileName);
                if (file.exists()) {
                    fileUrl = jqueryUiLocaleRelPath + jqueryUiLocalePrefix + modifiedDisplayCountry + jsFilePostFix;
                } else {
                    // use default language en as fallback
                    fileUrl = jqueryUiLocaleRelPath + jqueryUiLocalePrefix + defaultLocaleJquery + jsFilePostFix;
                }
            }
            jQueryLocaleFile.put(displayCountry, fileUrl);

            /*
             * Try to open the datetimepicker language file
             */
            fileName = componentRoot + dateTimePickerJsLocaleRelPath + dateTimePickerPrefix + strippedLocale + jsFilePostFix;
            file = FileUtil.getFile(fileName);

            if (file.exists()) {
                fileUrl = dateTimePickerJsLocaleRelPath + dateTimePickerPrefix + strippedLocale + jsFilePostFix;
            } else {
                // Try to guess a language
                fileName = componentRoot + dateTimePickerJsLocaleRelPath + dateTimePickerPrefix + modifiedDisplayCountry + jsFilePostFix;
                file = FileUtil.getFile(fileName);
                if (file.exists()) {
                    fileUrl = dateTimePickerJsLocaleRelPath + dateTimePickerPrefix + modifiedDisplayCountry + jsFilePostFix;
                } else {
                    // use default language en as fallback
                    fileUrl = dateTimePickerJsLocaleRelPath + dateTimePickerPrefix + defaultLocaleJquery + jsFilePostFix;
                }
            }
            dateTimePickerLocaleFile.put(displayCountry, fileUrl);

            /*
             * Try to open the Select 2 language file
             */
            fileName = componentRoot + select2LocaleRelPath + strippedLocale + jsFilePostFix;
            file = FileUtil.getFile(fileName);

            if (file.exists()) {
                fileUrl = select2LocaleRelPath + strippedLocale + jsFilePostFix;
            } else {
                // Try to guess a language
                fileName = componentRoot + select2LocaleRelPath + modifiedDisplayCountry + jsFilePostFix;
                file = FileUtil.getFile(fileName);
                if (file.exists()) {
                    fileUrl = select2LocaleRelPath + modifiedDisplayCountry + jsFilePostFix;
                } else {
                    // use default language en as fallback
                    fileUrl = select2LocaleRelPath + defaultLocaleJquery + jsFilePostFix;
                }
            }
            select2LocaleFile.put(displayCountry, fileUrl);
        }

        // check the template file
        String template = "themes/common-theme/template/JsLanguageFilesMapping.ftl";
        String output = "framework/common/src/main/java/org/apache/ofbiz/common/JsLanguageFilesMapping.java";
        Map<String, Object> mapWrapper = new HashMap<>();
        mapWrapper.put("datejs", dateJsLocaleFile);
        mapWrapper.put("jquery", jQueryLocaleFile);
        mapWrapper.put("validation", validationLocaleFile);
        mapWrapper.put("dateTime", dateTimePickerLocaleFile);
        mapWrapper.put("select2", select2LocaleFile);

        // some magic to create a new java file: render it as FTL
        Writer writer = new StringWriter();
        try {
            FreeMarkerWorker.renderTemplate(template, mapWrapper, writer);
            // write it as a Java file
            File file = new File(output);
            FileUtils.writeStringToFile(file, writer.toString(), encoding);
        } catch (IOException | TemplateException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage("CommonUiLabels", "CommonOutputFileCouldNotBeCreated",
                    UtilMisc.toMap("errorString", e.getMessage()), (Locale) context.get("locale")));
        }

        return result;
    }

}
