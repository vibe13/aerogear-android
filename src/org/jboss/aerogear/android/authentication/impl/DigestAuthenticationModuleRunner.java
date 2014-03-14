/**
 * JBoss, Home of Professional Open Source Copyright Red Hat, Inc., and
 * individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.jboss.aerogear.android.authentication.impl;

import android.util.Log;
import com.google.common.base.Strings;

import java.net.CookieManager;
import java.net.CookieStore;
import java.net.HttpCookie;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.jboss.aerogear.android.authentication.AuthenticationConfig;
import org.jboss.aerogear.android.http.HeaderAndBody;
import org.jboss.aerogear.android.http.HttpException;
import org.jboss.aerogear.android.http.HttpProvider;
import org.json.JSONObject;

public class DigestAuthenticationModuleRunner extends
        AbstractAuthenticationModuleRunner {

    private final String TAG = DigestAuthenticationModuleRunner.class.getSimpleName();

    private static String WWW_AUTHENTICATE_HEADER = "WWW-Authenticate";
    private static final String REALM = "realm";
    private static final String DOMAIN = "domain";
    private static final String NONCE = "nonce";
    private static final String STALE = "stale";
    private static final String ALGORITHM = "algorithm";
    private static final String QOP_OPTIONS = "qop";
    private static final String OPAQUE = "opaque";
    private String cnonce = UUID.randomUUID().toString();
    private int nonce_count = 0;
    private String nonce;
    private String qop;
    private String realm;
    private String domain;
    private String algorithm;
    private String stale;
    private String opaque;
    private String username;
    private String password;

    /**
     * @param baseURL the baseURL that all urls (login, enroll, etc) will be
     * appended to.
     * @param config a config object
     * @throws IllegalArgumentException if an endpoint can not be appended to
     * baseURL
     */
    public DigestAuthenticationModuleRunner(URL baseURL,
            AuthenticationConfig config) {
        super(baseURL, config);
    }

    @Override
    public HeaderAndBody onEnroll(final Map<String, String> userData) {
        HttpProvider provider = httpProviderFactory.get(enrollURL, timeout);
        String enrollData = new JSONObject(userData).toString();
        return provider.post(enrollData);
    }

    @Override
    public HeaderAndBody onLogin(final String username, final String password) {
        HttpProvider provider = httpProviderFactory.get(loginURL, timeout);
        try {
            provider.get();// Should not be logged in and throw an exception
            throw new IllegalStateException(
                    "Login Called on service which was already logged in.");
        } catch (HttpException exception) {
            // If an exception occured that was not a failed login
            if (exception.getStatusCode() != HttpURLConnection.HTTP_UNAUTHORIZED) {
                throw exception;
            }

            Map<String, String> authenticateHeaders = DigestHeaderUtils
                    .extractValues(exception.getHeaders().get(
                                    WWW_AUTHENTICATE_HEADER));
            realm = authenticateHeaders.get(REALM);
            domain = authenticateHeaders.get(DOMAIN);
            nonce = authenticateHeaders.get(NONCE);
            algorithm = authenticateHeaders.get(ALGORITHM);
            qop = authenticateHeaders.get(QOP_OPTIONS);
            stale = authenticateHeaders.get(STALE);
            opaque = authenticateHeaders.get(OPAQUE);
            this.username = username;
            this.password = password;

            checkQop(qop);
            checkAlgorithm(algorithm);
            try {
                provider.setDefaultHeader(
                        "Authorization",
                        getAuthorizationHeader(loginURL.toURI(), "GET",
                                new byte[]{}));
            } catch (URISyntaxException ex) {
                Log.e(TAG, ex.getMessage(), ex);
                throw new RuntimeException(ex);
            }

            return provider.get();
        }

    }

    @Override
    public void onLogout() {
        HttpProvider provider = httpProviderFactory.get(logoutURL, timeout);

        clear();

        CookieStore store = ((CookieManager) CookieManager.getDefault())
                .getCookieStore();
        List<HttpCookie> cookies = store.get(getBaseURI());

        for (HttpCookie cookie : cookies) {
            store.remove(getBaseURI(), cookie);
        }

        provider.post("");
    }

    private void clear() {
        realm = null;
        domain = null;
        nonce = null;
        algorithm = null;
        qop = null;
        stale = null;
        opaque = null;
        this.username = null;
        this.password = null;

    }

    /*
     * Currently only supports auth.
     */
    private void checkQop(String qop) {

        if (qop == null) {
            return;
        } else {
            for (String option : qop.split(",")) {
                if ("auth".equals(option)) {
                    this.qop = "auth";
                    return;
                }
            }
        }

        throw new IllegalArgumentException(String.format(
                "%s is not a supported qop type.", qop));

    }

    public String getAuthorizationHeader(URI uri, String method,
            byte[] entityBody) {
        nonce_count++;
        StringBuilder sb = new StringBuilder();
        String digestResponse;
        String HA1 = calculateHA1();
        String HA2 = calculateHA2(method, uri, entityBody);

        if (qop == null) {
            StringBuilder responseBuilder = new StringBuilder();
            responseBuilder.append(HA1).append(":").append(nonce).append(":")
                    .append(HA2);
            digestResponse = DigestHeaderUtils.computeMD5Hash(responseBuilder
                    .toString().getBytes());
        } else {
            StringBuilder responseBuilder = new StringBuilder();
            responseBuilder.append(HA1).append(":").append(nonce).append(":")
                    .append(nonce_count).append(":").append(cnonce).append(":")
                    .append(qop).append(":").append(HA2);
            digestResponse = DigestHeaderUtils.computeMD5Hash(responseBuilder
                    .toString().getBytes());
        }

        sb.append("Digest ").append("username=\"").append(username).append('"')
                .append(",realm=\"").append(realm).append('"')
                .append(",nonce=\"").append(nonce).append('"')
                .append(",uri=\"").append(uri.toString()).append('"')
                .append(",response=\"").append(digestResponse).append('"');
        if (!Strings.isNullOrEmpty(qop)) {
            sb.append(",qop=").append(qop).append(",nc=").append(nonce_count)
                    .append(",cnonce=\"").append(cnonce).append('"')
                    .append(",opaque=\"").append(opaque).append('"');
        }

        return sb.toString();
    }

    private void checkAlgorithm(String algorithm) {
        if (algorithm == null) {
            return;
        } else {
            for (String option : algorithm.split(",")) {
                if ("MD5".equals(option) || "MD5-sess".equals(option)) {
                    this.algorithm = option;
                    return;
                }
            }
        }

        throw new IllegalArgumentException(String.format(
                "%s is not a supported algorithm type.", algorithm));
    }

    private String calculateHA1() {
        StringBuilder a1Builder = new StringBuilder();
        a1Builder.append(username).append(":").append(realm).append(":")
                .append(password);
        if ("MD5-sess".equals(algorithm)) {
            String tempA1 = DigestHeaderUtils.computeMD5Hash(a1Builder
                    .toString().getBytes());
            a1Builder = new StringBuilder();
            a1Builder.append(tempA1).append(":").append(nonce).append(":")
                    .append(cnonce);
        }
        return DigestHeaderUtils
                .computeMD5Hash(a1Builder.toString().getBytes());
    }

    private String calculateHA2(String method, URI uri, byte[] entityBody) {
        StringBuilder a2Builder = new StringBuilder();
        if ("auth-int".equals(qop)) {
            a2Builder.append(method).append(":").append(uri).append(":")
                    .append(DigestHeaderUtils.computeMD5Hash(entityBody));
        } else {
            a2Builder.append(method).append(":").append(uri);
        }

        return DigestHeaderUtils
                .computeMD5Hash(a2Builder.toString().getBytes());

    }

    boolean retryLogin() {
        onLogin(username, password);

        return true;
    }
}
