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

import android.util.Log;
import java.net.URI;
import java.net.URL;
import java.util.Map;
import org.jboss.aerogear.android.Callback;
import org.jboss.aerogear.android.authentication.AbstractAuthenticationModule;
import static org.jboss.aerogear.android.authentication.AbstractAuthenticationModule.PASSWORD_PARAMETER_NAME;
import static org.jboss.aerogear.android.authentication.AbstractAuthenticationModule.USERNAME_PARAMETER_NAME;
import org.jboss.aerogear.android.authentication.AuthenticationConfig;
import org.jboss.aerogear.android.authentication.AuthorizationFields;
import org.jboss.aerogear.android.http.HeaderAndBody;

/**
 * This class provides Authentication using HTTP Digest
 *
 * As per the <a href="http://www.ietf.org/rfc/rfc2617.txt">HTTP RFC</a> this
 * class will cache credentials and consumed by {@link org.jboss.aerogear.android.pipeline.Pipe} requests. This
 * module assumes that credentials provided are valid and will never fail on {@link #login(java.lang.String, java.lang.String, org.jboss.aerogear.android.Callback)
 * }
 * or {@link AGSecurityAuthenticationModule#logout(org.jboss.aerogear.android.Callback)
 * }.
 *
 * {@link #enroll(java.util.Map, org.jboss.aerogear.android.Callback) } is not
 * supported and will always fail.
 *
 */
public class HttpDigestAuthenticationModule extends AbstractAuthenticationModule {

    private static final String TAG = AGSecurityAuthenticationModule.class.getSimpleName();

    private boolean isLoggedIn = false;

    private final DigestAuthenticationModuleRunner runner;

    /**
     *
     * @param baseURL the url that the other endpoints (enroll, login, eyc) will be appended to
     * @param config a config object
     * @throws IllegalArgumentException if an endpoint can not be appended to
     * baseURL
     */
    public HttpDigestAuthenticationModule(URL baseURL, AuthenticationConfig config) {
        this.runner = new DigestAuthenticationModuleRunner(baseURL, config);
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
        THREAD_POOL_EXECUTOR.execute(new Runnable() {
            @Override
            public void run() {
                HeaderAndBody result = null;
                Exception exception = null;

                try {
                    result = runner.onLogin(username, password);
                    isLoggedIn = true;
                } catch (Exception e) {
                    Log.e(TAG, "Error with Login", e);
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
        throw new UnsupportedOperationException("Not supported");
    }

    @Override
    public AuthorizationFields getAuthorizationFields(URI requestUri, String method, byte[] requestBody) {
        AuthorizationFields fields = new AuthorizationFields();
        fields.addHeader("Authorization", runner.getAuthorizationHeader(requestUri, method, requestBody));
        return fields;
    }

    @Override
    public boolean retryLogin() {
        return runner.retryLogin();
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public void login(Map<String, String> loginData, Callback<HeaderAndBody> callback) {
        login(loginData.get(USERNAME_PARAMETER_NAME), loginData.get(PASSWORD_PARAMETER_NAME), callback);
    }

}
