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
package org.jboss.aerogear.android.authentication.impl.loader;

import java.util.concurrent.CountDownLatch;

import org.jboss.aerogear.android.Callback;
import org.jboss.aerogear.android.authentication.AuthenticationModule;
import org.jboss.aerogear.android.http.HeaderAndBody;

import android.content.Context;
import android.content.Loader;
import android.util.Log;
import java.util.HashMap;
import java.util.Map;
import org.jboss.aerogear.android.authentication.AbstractAuthenticationModule;

/**
 * This class is a {@link Loader} which performs an login operation on behalf 
 * of an {@link AuthenticationModule}.
 */
public class LoginLoader extends AbstractAuthenticationLoader {

    private static final String TAG = LoginLoader.class.getSimpleName();

    private HeaderAndBody result = null;
    private final Map<String, String> loginData;
    
    @Deprecated
    /**
     * Use LoginLoader(Context, Callback, AuthenticationModule, Map) instead.
     */
    LoginLoader(Context context, Callback callback, AuthenticationModule module, String username, String password) {
        super(context, module, callback);
        Map<String, String> loginParams = new HashMap<String, String>();
        loginParams.put(AbstractAuthenticationModule.USERNAME_PARAMETER_NAME, username);
        loginParams.put(AbstractAuthenticationModule.PASSWORD_PARAMETER_NAME, password);
        this.loginData = loginParams;
    }

    
    LoginLoader(Context context, Callback callback, AuthenticationModule module, Map<String, String> loginData) {
        super(context, module, callback);
        this.loginData = new HashMap<String, String>(loginData);
    }

    
    @Override
    public HeaderAndBody loadInBackground() {
        final CountDownLatch latch = new CountDownLatch(1);
        module.login(loginData, new Callback<HeaderAndBody>() {

            @Override
            public void onSuccess(HeaderAndBody data) {
                result = data;
                latch.countDown();
            }

            @Override
            public void onFailure(Exception e) {
                LoginLoader.super.setException(e);
                latch.countDown();
            }
        });

        try {
            latch.await();
        } catch (InterruptedException ex) {
            Log.e(TAG, ex.getMessage(), ex);
        }

        return result;
    }

    @Override
    protected void onStartLoading() {
        if (module.isLoggedIn() && result != null) {
            deliverResult(result);
        } else {
            forceLoad();
        }
    }
    
}
