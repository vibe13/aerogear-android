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

import com.xtremelabs.robolectric.RobolectricTestRunner;
import org.jboss.aerogear.android.Provider;
import org.jboss.aerogear.android.authentication.AuthenticationConfig;
import org.jboss.aerogear.android.http.HeaderAndBody;
import org.jboss.aerogear.android.http.HttpException;
import org.jboss.aerogear.android.http.HttpProvider;
import org.jboss.aerogear.android.impl.helper.UnitTestUtils;
import org.jboss.aerogear.android.impl.http.HttpStubProvider;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.net.CookieManager;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import static org.jboss.aerogear.android.authentication.impl.AuthenticationModuleTest.LOGIN_PASSWORD;
import static org.jboss.aerogear.android.authentication.impl.AuthenticationModuleTest.PASSING_USERNAME;
import org.json.JSONException;
import org.json.JSONObject;

@RunWith(RobolectricTestRunner.class)
public class AGSecurityAuthenticationModuleTest implements AuthenticationModuleTest {

    private static final URL SIMPLE_URL;

    static {
        try {
            SIMPLE_URL = new URL("http://localhost:8080/todo-server");
            CookieManager.setDefault(new java.net.CookieManager());
        } catch (MalformedURLException ex) {
            throw new IllegalStateException(ex);
        }
    }

    @Test(timeout = 500L)
    public void testDefaultConstructor() throws Exception {
        AGSecurityAuthenticationModule module = new AGSecurityAuthenticationModule(
                SIMPLE_URL, new AuthenticationConfig());
        Object runner = UnitTestUtils.getPrivateField(module, "runner");
        HttpProvider provider = (HttpProvider) ((Provider)UnitTestUtils.getSuperPrivateField(runner,
                "httpProviderFactory")).get(SIMPLE_URL);
        Assert.assertEquals(SIMPLE_URL, provider.getUrl());

        Assert.assertEquals(SIMPLE_URL, module.getBaseURL());

    }

    @Test(timeout = 500L)
    public void loginFails() throws Exception {
        AGSecurityAuthenticationModule module = new AGSecurityAuthenticationModule(
                SIMPLE_URL, new AuthenticationConfig());
        final CountDownLatch latch = new CountDownLatch(1);
        Object runner = UnitTestUtils.getPrivateField(module, "runner");
        UnitTestUtils.setPrivateField(runner, "httpProviderFactory",
                new Provider<HttpProvider>() {
                    @Override
                    public HttpProvider get(Object... in) {
                        return new HttpStubProvider(SIMPLE_URL) {
                            @Override
                            public HeaderAndBody post(String ignore)
                                    throws RuntimeException {
                                try {
                                    throw new HttpException(new byte[1], 403);
                                } finally {
                                }
                            }
                        };
                    }
                });

        SimpleCallback callback = new SimpleCallback(latch);
        module.login(PASSING_USERNAME, LOGIN_PASSWORD, callback);

        latch.await();
        Assert.assertNotNull(callback.exception);
        Assert.assertFalse(module.isLoggedIn());
    }

    @Test(timeout = 500L)
    public void loginSucceedsLoginMap() throws IOException, NoSuchFieldException,
            InterruptedException, IllegalArgumentException,
            IllegalAccessException, JSONException {
        AGSecurityAuthenticationModule module = new AGSecurityAuthenticationModule(
                SIMPLE_URL, new AuthenticationConfig());
        final CountDownLatch latch = new CountDownLatch(1);
        final StringBuilder requestData = new StringBuilder();
        Object runner = UnitTestUtils.getPrivateField(module, "runner");
        UnitTestUtils.setPrivateField(runner, "httpProviderFactory",
                new Provider<HttpProvider>() {
                    @Override
                    public HttpProvider get(Object... in) {
                        return new HttpStubProvider(SIMPLE_URL) {
                            @Override
                            public HeaderAndBody post(String request)
                                    throws RuntimeException {
                                HashMap<String, Object> headers = new HashMap<String, Object>();
                                requestData.append(request);
                                return new HeaderAndBody(new byte[1], headers);

                            }
                        };
                    }
                });

        SimpleCallback callback = new SimpleCallback(latch);
        Map<String, String> loginData = new HashMap<String, String>(2);
        
        loginData.put("username", PASSING_USERNAME);
        
        loginData.put("password", LOGIN_PASSWORD);
        module.login(loginData, callback);
        latch.await();

        Assert.assertNull(callback.exception);
        Assert.assertNotNull(callback.data);
        Assert.assertTrue(module.isLoggedIn());
        
        JSONObject request = new JSONObject(requestData.toString());
        Assert.assertEquals(PASSING_USERNAME, request.getString("username"));
        Assert.assertEquals(LOGIN_PASSWORD, request.getString("password"));
        
    }
    
    @Test(timeout = 500L)
    public void loginSucceedsUsernamePassword() throws IOException, NoSuchFieldException,
            InterruptedException, IllegalArgumentException,
            IllegalAccessException, JSONException {
        AGSecurityAuthenticationModule module = new AGSecurityAuthenticationModule(
                SIMPLE_URL, new AuthenticationConfig());
        final CountDownLatch latch = new CountDownLatch(1);
        final StringBuilder requestData = new StringBuilder();
        Object runner = UnitTestUtils.getPrivateField(module, "runner");
        UnitTestUtils.setPrivateField(runner, "httpProviderFactory",
                new Provider<HttpProvider>() {
                    @Override
                    public HttpProvider get(Object... in) {
                        return new HttpStubProvider(SIMPLE_URL) {
                            @Override
                            public HeaderAndBody post(String request)
                                    throws RuntimeException {
                                HashMap<String, Object> headers = new HashMap<String, Object>();
                                requestData.append(request);
                                return new HeaderAndBody(new byte[1], headers);

                            }
                        };
                    }
                });

        SimpleCallback callback = new SimpleCallback(latch);
        module.login(PASSING_USERNAME, LOGIN_PASSWORD, callback);
        latch.await();

        Assert.assertNull(callback.exception);
        Assert.assertNotNull(callback.data);
        Assert.assertTrue(module.isLoggedIn());
        
        JSONObject request = new JSONObject(requestData.toString());
        Assert.assertEquals(PASSING_USERNAME, request.getString(AGSecurityAuthenticationModule.USERNAME_PARAMETER_NAME));
        Assert.assertEquals(LOGIN_PASSWORD, request.getString(AGSecurityAuthenticationModule.PASSWORD_PARAMETER_NAME));
        
    }

    @Test(timeout = 500L)
    public void enrollSucceeds() throws Exception {
        AGSecurityAuthenticationModule module = new AGSecurityAuthenticationModule(
                SIMPLE_URL, new AuthenticationConfig());
        final CountDownLatch latch = new CountDownLatch(1);
        Object runner = UnitTestUtils.getPrivateField(module, "runner");
        UnitTestUtils.setPrivateField(runner, "httpProviderFactory",
                new Provider<HttpProvider>() {
                    @Override
                    public HttpProvider get(Object... in) {
                        return new HttpStubProvider(SIMPLE_URL) {
                            @Override
                            public HeaderAndBody post(String enrollData)
                                    throws RuntimeException {
                                HashMap<String, Object> headers = new HashMap<String, Object>();
                                return new HeaderAndBody(new byte[1], headers);

                            }
                        };
                    }
                });
        SimpleCallback callback = new SimpleCallback(latch);

        Map<String, String> userData = new HashMap<String, String>();
        userData.put("username", PASSING_USERNAME);
        userData.put("password", ENROLL_PASSWORD);
        userData.put("firstname", "Summers");
        userData.put("lastname", "Pittman");
        userData.put("role", "admin");

        module.enroll(userData, callback);
        latch.await();
        Assert.assertNull(callback.exception);
        Assert.assertNotNull(callback.data);

        Assert.assertTrue(module.isLoggedIn());
    }

    @Test(timeout = 500L)
    public void logoutSucceeds() throws Exception {
        AGSecurityAuthenticationModule module = new AGSecurityAuthenticationModule(
                SIMPLE_URL, new AuthenticationConfig());
        
        final CountDownLatch latch = new CountDownLatch(1);
        SimpleCallback callback = new SimpleCallback(latch);

        Object runner = UnitTestUtils.getPrivateField(module, "runner");
        UnitTestUtils.setPrivateField(runner, "httpProviderFactory",
                new Provider<HttpProvider>() {
                    @Override
                    public HttpProvider get(Object... in) {
                        return new HttpStubProvider(SIMPLE_URL) {
                            @Override
                            public HeaderAndBody post(String ignore)
                                    throws RuntimeException {
                                    HashMap<String, Object> headers = new HashMap<String, Object>();
                                    return new HeaderAndBody(new byte[1],
                                            headers);
                            }
                        };
                    }
                });

        module.login(PASSING_USERNAME, LOGIN_PASSWORD, callback);

        latch.await();

        Assert.assertNull(callback.exception);
        Assert.assertNotNull(callback.data);
        Assert.assertTrue(module.isLoggedIn());


        final CountDownLatch latch2 = new CountDownLatch(1);
        VoidCallback voidCallback = new VoidCallback(latch2);
        
        UnitTestUtils.setPrivateField(runner, "httpProviderFactory",
                new Provider<HttpProvider>() {
                    @Override
                    public HttpProvider get(Object... in) {
                        return new HttpStubProvider(SIMPLE_URL) {
                            @Override
                            public HeaderAndBody post(String ignore)
                                    throws RuntimeException {
                                    HashMap<String, Object> headers = new HashMap<String, Object>();

                                    return new HeaderAndBody(new byte[1],
                                            headers);
                            }
                        };
                    }
                });

        module.logout(voidCallback);

        latch2.await();

        Assert.assertNull(voidCallback.exception);

        Assert.assertFalse(module.isLoggedIn());
    }
}
