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

import com.google.gson.JsonObject;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.jboss.aerogear.android.authentication.AuthenticationConfig;
import static org.jboss.aerogear.android.authentication.impl.AGSecurityAuthenticationModule.*;
import org.jboss.aerogear.android.http.HeaderAndBody;
import org.jboss.aerogear.android.http.HttpProvider;
import org.json.JSONObject;

class AGSecurityAuthenticationModuleRunner extends AbstractAuthenticationModuleRunner {

    private static final String TAG = AGSecurityAuthenticationModuleRunner.class.getSimpleName();

    /**
     * @param baseURL
     * @param config
     * @throws IllegalArgumentException if an endpoint can not be appended to
     * baseURL
     */
    public AGSecurityAuthenticationModuleRunner(URL baseURL, AuthenticationConfig config) {
        super(baseURL, config);
    }

    String buildLoginData(String username, String password) {
        JsonObject response = new JsonObject();
        response.addProperty("loginName", username);
        response.addProperty("password", password);
        return response.toString();
    }

    @Override
    public HeaderAndBody onEnroll(final Map<String, String> userData) {
        HttpProvider provider = httpProviderFactory.get(enrollURL, timeout);
        String enrollData = new JSONObject(userData).toString();
        return provider.post(enrollData);
    }

    @Override
    HeaderAndBody onLogin(String username, String password) {
        Map<String, String> loginData = new HashMap<String, String>(2);
        loginData.put(USERNAME_PARAMETER_NAME, username);
        loginData.put(PASSWORD_PARAMETER_NAME, password);
        return onLogin(loginData);
    }

    public HeaderAndBody onLogin(final Map<String, String> loginData) {
        HttpProvider provider = httpProviderFactory.get(loginURL, timeout);
        String loginRequest = new JSONObject(loginData).toString();
        return provider.post(loginRequest);
    }

    @Override
    public void onLogout() {
        HttpProvider provider = httpProviderFactory.get(logoutURL, timeout);
        provider.post("");
    }

}
