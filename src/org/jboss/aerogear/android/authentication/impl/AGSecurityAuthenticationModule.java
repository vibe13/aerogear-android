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

import org.jboss.aerogear.android.Callback;
import org.jboss.aerogear.android.authentication.AbstractAuthenticationModule;
import org.jboss.aerogear.android.authentication.AuthenticationConfig;
import org.jboss.aerogear.android.authentication.AuthorizationFields;
import org.jboss.aerogear.android.http.HeaderAndBody;

import android.util.Log;
import java.net.CookieManager;
import java.net.CookieStore;
import java.net.HttpCookie;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A module for authenticating with restful AG services.
 *
 * @see <a
 * href="https://github.com/aerogear/aerogear-security#endpoints-definition">AG
 * Security Endpoint Doc</a>
 */
public final class AGSecurityAuthenticationModule extends AbstractAuthenticationModule {

    private static final String TAG = AGSecurityAuthenticationModule.class.getSimpleName();

    private boolean isLoggedIn = false;

    private final AGSecurityAuthenticationModuleRunner runner;

    /**
     *
     * @param baseURL
     * @param config
     * @throws IllegalArgumentException if an endpoint can not be appended to
     * baseURL
     */
    public AGSecurityAuthenticationModule(URL baseURL, AuthenticationConfig config) {
        this.runner = new AGSecurityAuthenticationModuleRunner(baseURL, config);
    }

    @Override
    public URL getBaseURL() {
        return runner.getBaseURL();
    }

    @Override
    public String getLoginEndpoint() {
        return runner.getLoginEndpoint();
    }

    @Override
    public String getLogoutEndpoint() {
        return runner.getLogoutEndpoint();
    }

    @Override
    public String getEnrollEndpoint() {
        return runner.getEnrollEndpoint();
    }

    @Override
    public void enroll(final Map<String, String> userData,
            final Callback<HeaderAndBody> callback) {
        THREAD_POOL_EXECUTOR.execute(new Runnable() {
            @Override
            public void run() {
                HeaderAndBody result = null;
                Exception exception = null;
                try {
                    result = runner.onEnroll(userData);
                    isLoggedIn = true;
                } catch (Exception e) {
                    Log.e(TAG, "error enrolling", e);
                    exception = e;
                }

                if (exception == null) {
                    callback.onSuccess(result);
                } else {
                    callback.onFailure(exception);
                }

            }
        });

    }

    @Override
    public void login(final String username, final String password,
            final Callback<HeaderAndBody> callback) {
        Map<String, String> loginData = new HashMap<String, String>(2);
        loginData.put(USERNAME_PARAMETER_NAME, username);
        loginData.put(PASSWORD_PARAMETER_NAME, password);
        login(loginData, callback);
    }

    @Override
    public void login(final Map<String, String> loginData,
            final Callback<HeaderAndBody> callback) {
        THREAD_POOL_EXECUTOR.execute(new Runnable() {
            @Override
            public void run() {
                HeaderAndBody result = null;
                Exception exception = null;
                try {
                    result = runner.onLogin(loginData);
                    isLoggedIn = true;
                } catch (Exception e) {
                    Log.e(TAG, "error logging in", e);
                    exception = e;
                }

                if (exception == null) {
                    callback.onSuccess(result);
                } else {
                    callback.onFailure(exception);
                }

            }
        });

    }

    @Override
    public void logout(final Callback<Void> callback) {
        THREAD_POOL_EXECUTOR.execute(new Runnable() {
            @Override
            public void run() {
                Exception exception = null;
                try {
                    runner.onLogout();

                    CookieStore store = ((CookieManager) CookieManager.getDefault()).getCookieStore();
                    List<HttpCookie> cookies = store.get(getBaseURL().toURI());

                    for (HttpCookie cookie : cookies) {
                        store.remove(getBaseURL().toURI(), cookie);
                    }

                    isLoggedIn = false;
                } catch (Exception e) {
                    Log.e(TAG, "Error with Login", e);
                    exception = e;
                }
                if (exception == null) {
                    callback.onSuccess(null);
                } else {
                    callback.onFailure(exception);
                }
            }
        });

    }

    @Override
    public boolean isLoggedIn() {
        return isLoggedIn;
    }

    @Override
    public AuthorizationFields getAuthorizationFields() {
        return new AuthorizationFields();
    }

    @Override
    public AuthorizationFields getAuthorizationFields(URI requestUri, String method, byte[] requestBody) {
        return getAuthorizationFields();
    }

    @Override
    public boolean retryLogin() {
        return false;
    }

}
