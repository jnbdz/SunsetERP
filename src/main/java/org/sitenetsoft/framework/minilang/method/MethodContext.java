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
package org.sitenetsoft.framework.minilang.method;

//import io.quarkus.security.identity.SecurityIdentity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.SecurityContext;
import org.sitenetsoft.framework.base.util.Debug;
import org.sitenetsoft.framework.base.util.UtilHttp;
import org.sitenetsoft.framework.base.util.UtilMisc;
import org.sitenetsoft.framework.base.util.collections.FlexibleMapAccessor;
import org.sitenetsoft.framework.base.util.string.FlexibleStringExpander;
import org.sitenetsoft.framework.entity.Delegator;
import org.sitenetsoft.framework.entity.GenericValue;
import org.sitenetsoft.framework.security.Security;
import org.sitenetsoft.framework.service.DispatchContext;
import org.sitenetsoft.framework.service.LocalDispatcher;

//import jakarta.servlet.http.HttpServletRequest;
//import jakarta.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

/**
 * A container for the Mini-language script engine state.
 */
@ApplicationScoped
public final class MethodContext {

    public static final int EVENT = 1;
    public static final int SERVICE = 2;

    @Inject
    Delegator delegator;

    @Inject
    LocalDispatcher dispatcher;

    //@Inject
    //SecurityIdentity identity;

    @Context
    SecurityContext securityContext; // JAX-RS security context, if you need user or security info

    private Map<String, Object> env = new HashMap<>();
    private ClassLoader loader;
    private Locale locale;
    private int methodType;
    private Map<String, Object> parameters;
    private HttpServletRequest request = null;
    private HttpServletResponse response = null;
    private Map<String, Object> results = new HashMap<>();
    private Security security;
    private TimeZone timeZone;
    private int traceCount = 0;
    private int traceLogLevel = Debug.INFO;
    private GenericValue userLogin;

    public MethodContext(DispatchContext ctx, Map<String, ? extends Object> context, ClassLoader loader) {
        this.methodType = MethodContext.SERVICE;
        this.parameters = UtilMisc.makeMapWritable(context);
        this.loader = loader;
        this.locale = (Locale) context.get("locale");
        this.timeZone = (TimeZone) context.get("timeZone");
        this.dispatcher = ctx.getDispatcher();
        this.delegator = ctx.getDelegator();
        this.security = ctx.getSecurity();
        this.userLogin = (GenericValue) context.get("userLogin");
        //this.userLogin = (GenericValue) identity.getPrincipal().getName();
        if (this.loader == null) {
            try {
                this.loader = Thread.currentThread().getContextClassLoader();
            } catch (SecurityException e) {
                this.loader = this.getClass().getClassLoader();
            }
        }
    }

    public MethodContext(ClassLoader loader) {
        this.methodType = MethodContext.EVENT;

        // Assuming you've moved parameters, locale, timeZone, etc. to the JAX-RS context or other parts of the app
        this.parameters = new HashMap<>(); // Modify this according to where you now store parameters.
        this.loader = loader;

        // Commented out because these references to HttpServletRequest and HttpServletResponse are removed.
        // this.request = request;
        // this.response = response;

        // Locale and TimeZone should be set based on your application's design.
        // If they're in the JAX-RS context, you can get them from there.
        // Otherwise, you may set defaults or other mechanisms to obtain them.
        this.locale = Locale.getDefault(); // Use default or your mechanism
        this.timeZone = TimeZone.getDefault(); // Use default or your mechanism

        // You might need to get attributes like "dispatcher", "delegator", etc. from somewhere else.
        // For instance, if you store them as singletons or in the app context, retrieve them from there.
        // The below code assumes you have some mechanism to get them.
        /*this.dispatcher = getDispatcherFromSomeMechanism();
        this.delegator = getDelegatorFromSomeMechanism();
        this.security = getSecurityFromSomeMechanism();*/

        // If userLogin info is in the JAX-RS security context, get it from there.
        /*this.userLogin = getUserLoginFromSecurityContext(securityContext);*/

        if (this.loader == null) {
            try {
                this.loader = Thread.currentThread().getContextClassLoader();
            } catch (SecurityException e) {
                this.loader = this.getClass().getClassLoader();
            }
        }
    }

    /**
     * This is a very simple constructor which assumes the needed objects (dispatcher, delegator, security, request, response,
     * etc) are in the context. Will result in calling method as a
     * service or event, as specified.
     */
    public MethodContext(Map<String, ? extends Object> context, ClassLoader loader, int methodType) {
        this.methodType = methodType;
        this.parameters = UtilMisc.makeMapWritable(context);
        this.loader = loader;
        this.locale = (Locale) context.get("locale");
        this.timeZone = (TimeZone) context.get("timeZone");
        this.dispatcher = (LocalDispatcher) context.get("dispatcher");
        this.delegator = (Delegator) context.get("delegator");
        this.security = (Security) context.get("security");
        this.userLogin = (GenericValue) context.get("userLogin");
        if (methodType == MethodContext.EVENT) {
            this.request = (HttpServletRequest) context.get("request");
            this.response = (HttpServletResponse) context.get("response");
            if (this.locale == null) {
                this.locale = UtilHttp.getLocale(request);
            }
            if (this.timeZone == null) {
                this.timeZone = UtilHttp.getTimeZone(request);
            }
            // make sure the delegator and other objects are in place, getting from
            // request if necessary; assumes this came through the ControlServlet
            // or something similar
            if (this.request != null) {
                if (this.dispatcher == null) {
                    this.dispatcher = (LocalDispatcher) this.request.getAttribute("dispatcher");
                }
                if (this.delegator == null) {
                    this.delegator = (Delegator) this.request.getAttribute("delegator");
                }
                if (this.security == null) {
                    this.security = (Security) this.request.getAttribute("security");
                }
                if (this.userLogin == null) {
                    this.userLogin = (GenericValue) this.request.getSession().getAttribute("userLogin");
                }
            }
        }
        if (this.loader == null) {
            try {
                this.loader = Thread.currentThread().getContextClassLoader();
            } catch (SecurityException e) {
                this.loader = this.getClass().getClassLoader();
            }
        }
    }

    /*private LocalDispatcher getDispatcherFromSomeMechanism() {
        // You might get the dispatcher from a service, a context, or some global state, etc.
        // This is just a mock example.
        return LocalDispatcher.getInstance(); // assuming there's a static method named getInstance() in LocalDispatcher
    }

    private Delegator getDelegatorFromSomeMechanism() {
        // Similarly, retrieve the delegator. The method used here is fictional and meant as an example.
        return DelegatorService.getDelegator(); // replace with actual method
    }*/

    /*private Security getSecurityFromSomeMechanism() {
        // As with the dispatcher and delegator, this depends on your application's design.
        return SecurityService.getCurrentSecurityInstance(); // replace with actual method
    }*/

    /*private GenericValue getUserLoginFromSecurityContext(SecurityContext securityContext) {
        // Here, you'd extract the user info from the JAX-RS SecurityContext, if applicable.
        // Assuming GenericValue can be created from a principal's name:
        if (securityContext != null && securityContext.getUserPrincipal() != null) {
            String username = securityContext.getUserPrincipal().getName();
            return new GenericValue(username); // replace with a more appropriate constructor or method
        }
        return null;
    }*/

    public Delegator getDelegator() {
        return this.delegator;
    }

    public LocalDispatcher getDispatcher() {
        return this.dispatcher;
    }

    public <T> T getEnv(FlexibleMapAccessor<T> fma) {
        return fma.get(this.env);
    }

    /**
     * Gets the named value from the environment. Supports the "." (dot) syntax to access Map members and the "[]" (bracket)
     * syntax to access List entries. This value is expanded, supporting the
     * insertion of other environment values using the "${}" notation.
     * @param key
     *            The name of the environment value to get. Can contain "." and "[]" syntax elements as described above.
     * @return The environment value if found, otherwise null.
     */
    public <T> T getEnv(String key) {
        String ekey = FlexibleStringExpander.expandString(key, this.env);
        FlexibleMapAccessor<T> fma = FlexibleMapAccessor.getInstance(ekey);
        return this.getEnv(fma);
    }

    public Map<String, Object> getEnvMap() {
        return this.env;
    }

    public ClassLoader getLoader() {
        return this.loader;
    }

    public Locale getLocale() {
        return this.locale;
    }

    public int getMethodType() {
        return this.methodType;
    }

    public Object getParameter(String key) {
        return this.parameters.get(key);
    }

    public Map<String, Object> getParameters() {
        return this.parameters;
    }

    public HttpServletRequest getRequest() {
        return this.request;
    }

    public HttpServletResponse getResponse() {
        return this.response;
    }

    public Object getResult(String key) {
        return this.results.get(key);
    }

    public Map<String, Object> getResults() {
        return this.results;
    }

    public Security getSecurity() {
        return this.security;
    }

    public TimeZone getTimeZone() {
        return this.timeZone;
    }

    public int getTraceLogLevel() {
        return this.traceLogLevel;
    }

    public GenericValue getUserLogin() {
        return this.userLogin;
    }

    public boolean isTraceOn() {
        return this.traceCount > 0;
    }

    /**
     * Calls putEnv for each entry in the Map, thus allowing for the additional flexibility in naming supported in that method.
     */
    public void putAllEnv(Map<String, ? extends Object> values) {
        for (Map.Entry<String, ? extends Object> entry : values.entrySet()) {
            this.putEnv(entry.getKey(), entry.getValue());
        }
    }

    public <T> void putEnv(FlexibleMapAccessor<T> fma, T value) {
        fma.put(this.env, value);
    }

    /**
     * Puts the named value in the environment. Supports the "." (dot) syntax to access Map members and the "[]" (bracket) syntax to access List
     * entries. If the brackets for a list are empty the value will be appended to end of the list, otherwise the value will be set in the position
     * of the number in the brackets. If a "+" (plus sign) is included inside the square brackets before the index number the value will
     * inserted/added at that index instead of set at that index. This value is expanded, supporting the insertion of other environment values
     * using the "${}" notation.
     * @param key
     *            The name of the environment value to get. Can contain "." syntax elements as described above.
     * @param value
     *            The value to set in the named environment location.
     */
    public <T> void putEnv(String key, T value) {
        String ekey = FlexibleStringExpander.expandString(key, this.env);
        FlexibleMapAccessor<T> fma = FlexibleMapAccessor.getInstance(ekey);
        this.putEnv(fma, value);
    }

    public void putParameter(String key, Object value) {
        this.parameters.put(key, value);
    }

    public void putResult(String key, Object value) {
        this.results.put(key, value);
    }

    public <T> T removeEnv(FlexibleMapAccessor<T> fma) {
        return fma.remove(this.env);
    }

    /**
     * Removes the named value from the environment. Supports the "." (dot) syntax to access Map members and the "[]" (bracket) syntax to access
     * List entries. This value is expanded, supporting the
     * insertion of other environment values using the "${}" notation.
     * @param key
     *            The name of the environment value to get. Can contain "." syntax elements as described above.
     */
    public <T> T removeEnv(String key) {
        String ekey = FlexibleStringExpander.expandString(key, this.env);
        FlexibleMapAccessor<T> fma = FlexibleMapAccessor.getInstance(ekey);
        return this.removeEnv(fma);
    }

    public void setTraceOff() {
        if (this.traceCount > 0) {
            this.traceCount--;
        }
    }

    public void setTraceOn(int logLevel) {
        if (this.traceCount == 0) {
            // Outermost trace element sets the logging level
            this.traceLogLevel = logLevel;
        }
        this.traceCount++;
    }

    public void setUserLogin(GenericValue userLogin, String userLoginEnvName) {
        this.userLogin = userLogin;
        this.putEnv(userLoginEnvName, userLogin);
    }
}
