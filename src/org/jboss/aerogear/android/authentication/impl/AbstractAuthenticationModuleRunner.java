/**
 * JBoss, Home of Professional Open Source
 * Copyright Red Hat, Inc., and individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 	http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.aerogear.android.authentication.impl;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Map;
import org.jboss.aerogear.android.Provider;
import org.jboss.aerogear.android.authentication.AuthenticationConfig;
import org.jboss.aerogear.android.http.HeaderAndBody;
import org.jboss.aerogear.android.http.HttpProvider;
import org.jboss.aerogear.android.impl.core.HttpProviderFactory;
import org.jboss.aerogear.android.impl.util.UrlUtils;

import android.util.Log;

public abstract class AbstractAuthenticationModuleRunner {

    private static final String TAG = AbstractAuthenticationModuleRunner.class.getSimpleName();
    protected final URL baseURL;
    protected final String enrollEndpoint;
    protected final URL enrollURL;
    protected final Provider<HttpProvider> httpProviderFactory = new HttpProviderFactory();
    protected final String loginEndpoint;
    protected final URL loginURL;
    protected final String logoutEndpoint;
    protected final URL logoutURL;
    protected final Integer timeout;

    /**
     * @param baseURL
     * @param config
     * @throws IllegalArgumentException if an endpoint can not be appended to
     * baseURL
     */
    public AbstractAuthenticationModuleRunner(URL baseURL, AuthenticationConfig config) {
        this.baseURL = baseURL;
        this.loginEndpoint = config.getLoginEndpoint();
        this.logoutEndpoint = config.getLogoutEndpoint();
        this.enrollEndpoint = config.getEnrollEndpoint();

        this.loginURL = UrlUtils.appendToBaseURL(baseURL, loginEndpoint);
        this.logoutURL = UrlUtils.appendToBaseURL(baseURL, logoutEndpoint);
        this.enrollURL = UrlUtils.appendToBaseURL(baseURL, enrollEndpoint);

        this.timeout = config.getTimeout();

    }

    public URL getBaseURL() {
        return baseURL;
    }

    public URI getBaseURI() {
        try {
            return baseURL.toURI();
        } catch (URISyntaxException e) {
            Log.e(TAG, e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    public String getEnrollEndpoint() {
        return enrollEndpoint;
    }

    public String getLoginEndpoint() {
        return loginEndpoint;
    }

    public String getLogoutEndpoint() {
        return logoutEndpoint;
    }

    abstract HeaderAndBody onEnroll(final Map<String, String> userData);

    abstract HeaderAndBody onLogin(final String username, final String password);

    abstract void onLogout();
}
