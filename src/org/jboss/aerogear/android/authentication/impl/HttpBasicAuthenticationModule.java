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

import java.net.CookieManager;
import java.net.CookieStore;
import java.net.HttpCookie;
import java.net.PasswordAuthentication;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.aerogear.android.Callback;
import org.jboss.aerogear.android.authentication.AbstractAuthenticationModule;
import org.jboss.aerogear.android.authentication.AuthorizationFields;
import org.jboss.aerogear.android.http.HeaderAndBody;
import org.jboss.aerogear.android.pipeline.Pipe;

import android.util.Base64;
import android.util.Pair;
import java.net.URI;

/**
 * This class provides Authentication using HTTP Basic
 *
 * As per the <a href="http://www.ietf.org/rfc/rfc2617.txt">HTTP RFC</a> this
 * class will cache credentials and consumed by {@link Pipe} requests. This
 * module assumes that credentials provided are valid and will never fail on {@link #login(java.lang.String, java.lang.String, org.jboss.aerogear.android.Callback)
 * }
 * or {@link AGSecurityAuthenticationModule#logout(org.jboss.aerogear.android.Callback)
 * }.
 *
 * {@link #enroll(java.util.Map, org.jboss.aerogear.android.Callback) } is not
 * supported and will always fail.
 *
 */
public class HttpBasicAuthenticationModule extends AbstractAuthenticationModule {

    private final static String BASIC_HEADER = "Authorization";
    private final static String AUTHORIZATION_METHOD = "Basic";
    private final String loginEndpoint = "";
    private final String logoutEndpoint = "";
    private final String enrollEndpoint = "";
    private final URL baseURL;
    private boolean isLoggedIn = false;
    private PasswordAuthentication auth = new PasswordAuthentication("", new char[] {});

    /**
     * @param baseURL The base URL shared by a Pipe.
     */
    public HttpBasicAuthenticationModule(URL baseURL) {
        this.baseURL = baseURL;
    }

    @Override
    public URL getBaseURL() {
        return baseURL;
    }

    @Override
    public String getLoginEndpoint() {
        return loginEndpoint;
    }

    @Override
    public String getLogoutEndpoint() {
        return logoutEndpoint;
    }

    @Override
    public String getEnrollEndpoint() {
        return enrollEndpoint;
    }

    @Override
    public boolean isLoggedIn() {
        return isLoggedIn;
    }

    /**
     * This method stores username and password. Additionally, it sets
     * {@link #isLoggedIn} to true and calls {@link Callback#onSuccess(java.lang.Object)
     * }.
     *
     * @param username
     * @param password
     * @param callback
     */
    @Override
    public void login(String username, String password, final Callback<HeaderAndBody> callback) {
        isLoggedIn = true;
        auth = new PasswordAuthentication(username, password.toCharArray());
        THREAD_POOL_EXECUTOR.execute(new Runnable() {
            @Override
            public void run() {
                callback.onSuccess(new HeaderAndBody(new byte[] {}, new HashMap<String, Object>(1)));
            }
        });

    }

    /**
     *
     * This method clears the username and password from the module, sets
     * isLoggedIn to false, and removes all cookies associated with
     * {@link #baseURL}
     *
     * This method always calls {@link Callback#onSuccess(java.lang.Object) }
     *
     * @param callback
     */
    @Override
    public void logout(final Callback<Void> callback) {
        clearPassword(auth.getPassword());
        auth = new PasswordAuthentication("", new char[] {});
        isLoggedIn = false;

        THREAD_POOL_EXECUTOR.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    CookieStore store = ((CookieManager) CookieManager.getDefault()).getCookieStore();
                    List<HttpCookie> cookies = store.get(baseURL.toURI());

                    for (HttpCookie cookie : cookies) {
                        store.remove(baseURL.toURI(), cookie);
                    }

                    callback.onSuccess((Void) null);
                } catch (URISyntaxException e) {
                    callback.onFailure(e);
                }

            }
        });

    }

    /**
     *
     * Enrolling is not supported using http basic.
     *
     * This method will call {@link Callback#onFailure(java.lang.Exception)} and pass
     * it a UnsupportedOperationException.
     *
     * @param userData this value is ignored.
     * @param callback
     */
    @Override
    public void enroll(Map<String, String> userData, final Callback<HeaderAndBody> callback) {
        THREAD_POOL_EXECUTOR.execute(new Runnable() {
            @Override
            public void run() {
                callback.onFailure(new UnsupportedOperationException());
            }
        });

    }

    @Override
    public AuthorizationFields getAuthorizationFields() {
        AuthorizationFields fields = new AuthorizationFields();
        List<Pair<String, String>> headerList = new ArrayList<Pair<String, String>>(1);
        headerList.add(new Pair<String, String>(BASIC_HEADER, getHashedAuth()));
        fields.setHeaders(headerList);
        return fields;
    }

    @Override
    public AuthorizationFields getAuthorizationFields(URI requestUri, String method, byte[] requestBody) {
        return getAuthorizationFields();
    }

    @Override
    public boolean retryLogin() {
        return false;
    }

    /**
     * HTTP Basic defines a base 64 encoded hash to be pass as a header to serve
     * as authentication. This method calculates the value of that header.
     *
     * @return the http basic hash of the username and password
     */
    private String getHashedAuth() {
        StringBuilder headerValueBuilder = new StringBuilder(AUTHORIZATION_METHOD).append(" ");
        String unhashedCredentials = new StringBuilder(auth.getUserName()).append(":").append(auth.getPassword()).toString();
        String hashedCrentials = Base64.encodeToString(unhashedCredentials.getBytes(), Base64.DEFAULT | Base64.NO_WRAP);
        return headerValueBuilder.append(hashedCrentials).toString();
    }

    /**
     * This method replaces the characters in a character array with '0'.
     *
     * @param password a character array, usually a password
     */
    private void clearPassword(char[] password) {
        for (int i = 0; i < password.length; i++) {
            password[i] = '0';
        }
    }
}
