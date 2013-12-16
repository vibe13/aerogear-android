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

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.jboss.aerogear.android.Callback;
import org.jboss.aerogear.android.authentication.AuthenticationModule;
import org.jboss.aerogear.android.authentication.AuthorizationFields;
import org.jboss.aerogear.android.authentication.impl.loader.support.SupportAuthenticationModuleAdapter;
import org.jboss.aerogear.android.http.HeaderAndBody;
import org.jboss.aerogear.android.pipeline.AbstractActivityCallback;
import org.jboss.aerogear.android.pipeline.AbstractFragmentCallback;

import android.app.Activity;
import android.app.Fragment;
import android.app.LoaderManager;
import android.content.Context;
import android.content.Loader;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.FragmentActivity;
import android.util.Log;

import com.google.common.base.Objects;
import com.google.common.collect.Collections2;
import com.google.common.collect.Maps;
import java.net.URI;
import java.util.Collections;
import org.jboss.aerogear.android.authentication.AbstractAuthenticationModule;
import static org.jboss.aerogear.android.authentication.impl.loader.LoaderAuthenticationModule.CALLBACK;
import static org.jboss.aerogear.android.authentication.impl.loader.LoaderAuthenticationModule.METHOD;
import static org.jboss.aerogear.android.authentication.impl.loader.LoaderAuthenticationModule.PASSWORD;
import static org.jboss.aerogear.android.authentication.impl.loader.LoaderAuthenticationModule.USERNAME;

/**
 * This class manages the relationship between Android's Loader framework and
 * requests to Authentication. This class acts as a proxy for an
 * {@link AuthenticationModule} instance.
 *
 * This class instantiates the Loaders from android.content and will not work on
 * devices &lt; Android 3.0. For these devices see
 * {@link SupportAuthenticationModuleAdapter }
 */
public class AuthenticationModuleAdapter implements LoaderAuthenticationModule, LoaderManager.LoaderCallbacks<HeaderAndBody> {

    private static final String TAG = AuthenticationModuleAdapter.class.getSimpleName();

    public static enum Methods {

        LOGIN, LOGOUT, ENROLL
    };

    private final Context applicationContext;
    private final AuthenticationModule module;
    private final LoaderManager manager;
    private final Activity activity;
    private final Fragment fragment;
    private final String name;
    private final Handler handler;

    public AuthenticationModuleAdapter(Activity activity, AuthenticationModule module, String name) {
        this.module = module;
        this.manager = activity.getLoaderManager();
        this.applicationContext = activity.getApplicationContext();
        this.activity = activity;
        this.fragment = null;
        this.name = name;
        this.handler = new Handler(Looper.getMainLooper());
    }

    public AuthenticationModuleAdapter(Fragment fragment, Context applicationContext, AuthenticationModule module, String name) {
        this.module = module;
        this.manager = fragment.getLoaderManager();
        this.applicationContext = applicationContext;
        this.activity = null;
        this.fragment = fragment;
        this.name = name;
        this.handler = new Handler(Looper.getMainLooper());
    }

    @Override
    public URL getBaseURL() {
        return module.getBaseURL();
    }

    @Override
    public String getLoginEndpoint() {
        return module.getLoginEndpoint();
    }

    @Override
    public String getLogoutEndpoint() {
        return module.getLogoutEndpoint();
    }

    @Override
    public String getEnrollEndpoint() {
        return module.getEnrollEndpoint();
    }

    @Override
    public void enroll(Map<String, String> userData, Callback<HeaderAndBody> callback) {
        int id = Objects.hashCode(name, userData, callback);
        Bundle bundle = new Bundle();
        bundle.putSerializable(CALLBACK, callback);
        bundle.putSerializable(PARAMS, new HashMap(userData));
        bundle.putSerializable(METHOD, AuthenticationModuleAdapter.Methods.ENROLL);
        manager.initLoader(id, bundle, this);
    }

    @Override
    public void login(String username, String password, Callback<HeaderAndBody> callback) {
        Map<String, String> loginData = new HashMap<String, String>(4);
        loginData.put(AbstractAuthenticationModule.USERNAME_PARAMETER_NAME, username);
        loginData.put(AbstractAuthenticationModule.PASSWORD_PARAMETER_NAME, password);
        login(loginData, callback);
    }

    @Override
    public void login(Map<String, String> loginData, Callback<HeaderAndBody> callback) {
        int id = Objects.hashCode(name, loginData, callback);
        Bundle bundle = new Bundle();
        Bundle loginBundle = new Bundle();

        for (Map.Entry<String, String> entry : loginData.entrySet()) {
            loginBundle.putString(entry.getKey(), entry.getValue());
        }

        bundle.putSerializable(CALLBACK, callback);
        bundle.putBundle(PARAMS, loginBundle);
        bundle.putSerializable(METHOD, AuthenticationModuleAdapter.Methods.LOGIN);

        if (manager.getLoader(id) != null && !module.isLoggedIn()) {
            manager.restartLoader(id, bundle, this);
        } else {
            manager.initLoader(id, bundle, this);
        }
    }

    @Override
    public void logout(Callback<Void> callback) {
        int id = Objects.hashCode(name, callback);
        Bundle bundle = new Bundle();
        bundle.putSerializable(CALLBACK, callback);
        bundle.putSerializable(METHOD, AuthenticationModuleAdapter.Methods.LOGOUT);
        if (manager.getLoader(id) != null && module.isLoggedIn()) {
            manager.restartLoader(id, bundle, this);
        } else {
            manager.initLoader(id, bundle, this);
        }
    }

    @Override
    public boolean isLoggedIn() {
        return module.isLoggedIn();
    }

    @Override
    public AuthorizationFields getAuthorizationFields() {
        return module.getAuthorizationFields();
    }

    @Override
    public AuthorizationFields getAuthorizationFields(URI requestUri, String method, byte[] requestBody) {
        return module.getAuthorizationFields(requestUri, method, requestBody);
    }

    @Override
    public boolean retryLogin() {
        return module.retryLogin();
    }

    @Override
    public Loader<HeaderAndBody> onCreateLoader(int id, Bundle bundle) {
        AuthenticationModuleAdapter.Methods method = (AuthenticationModuleAdapter.Methods) bundle.get(METHOD);
        Callback callback = (Callback) bundle.get(CALLBACK);
        Loader loader = null;
        switch (method) {
        case LOGIN: {
            Bundle loginBundle = bundle.getBundle(PARAMS);
            Map<String, String> loginParams = new HashMap<String, String>(loginBundle.size());
            for (String key : loginBundle.keySet()) {
                loginParams.put(key, loginBundle.getString(key));
            }
            loader = new LoginLoader(applicationContext, callback, module, loginParams);
        }
            break;
        case LOGOUT: {
            loader = new LogoutLoader(applicationContext, callback, module);
        }
            break;
        case ENROLL: {
            Map<String, String> params = (Map<String, String>) bundle.getSerializable(PARAMS);
            loader = new EnrollLoader(applicationContext, callback, module, params);
        }
            break;
        }
        return loader;
    }

    /**
     * This method will call the Callback for a enroll, login, or logout method
     * on the main thread of the application. If a callback is an instance of
     * {@link AbstractFragmentCallback} or {@link AbstractActivityCallback} then
     * it will also configure the reference to {@link Fragment} or
     * {@link FragmentActivity} for the callback.
     */
    @Override
    public void onLoadFinished(Loader<HeaderAndBody> loader, final HeaderAndBody data) {
        if (!(loader instanceof AbstractAuthenticationLoader)) {
            Log.e(TAG, "Adapter is listening to loaders which it doesn't support");
            throw new IllegalStateException("Adapter is listening to loaders which it doesn't support");
        } else {
            final AbstractAuthenticationLoader modernLoader = (AbstractAuthenticationLoader) loader;
            handler.post(new CallbackHandler(this, modernLoader, data));
        }
    }

    @Override
    public void onLoaderReset(Loader<HeaderAndBody> loader) {
        //Do nothing, should call logout on module manually.
    }

    private void fragmentSuccess(Callback<HeaderAndBody> typelessCallback, HeaderAndBody data) {
        AbstractFragmentCallback callback = (AbstractFragmentCallback) typelessCallback;
        callback.setFragment(fragment);
        callback.onSuccess(data);
        callback.setFragment(null);
    }

    private void fragmentFailure(Callback<HeaderAndBody> typelessCallback, Exception exception) {
        AbstractFragmentCallback callback = (AbstractFragmentCallback) typelessCallback;
        callback.setFragment(fragment);
        callback.onFailure(exception);
        callback.setFragment(null);
    }

    private void activitySuccess(Callback<HeaderAndBody> typelessCallback, HeaderAndBody data) {
        AbstractActivityCallback callback = (AbstractActivityCallback) typelessCallback;
        callback.setActivity(activity);
        callback.onSuccess(data);
        callback.setActivity(null);
    }

    private void activityFailure(Callback<HeaderAndBody> typelessCallback, Exception exception) {
        AbstractActivityCallback callback = (AbstractActivityCallback) typelessCallback;
        callback.setActivity(activity);
        callback.onFailure(exception);
        callback.setActivity(null);
    }

    final static class CallbackHandler implements Runnable {

        private final AuthenticationModuleAdapter adapter;
        private final AbstractAuthenticationLoader modernLoader;
        private final HeaderAndBody data;

        public CallbackHandler(AuthenticationModuleAdapter adapter,
                AbstractAuthenticationLoader loader, HeaderAndBody data) {
            super();
            this.adapter = adapter;
            this.modernLoader = loader;
            this.data = data;
        }

        @Override
        public void run() {
            if (modernLoader.hasException()) {
                final Exception exception = modernLoader.getException();
                Log.e(TAG, exception.getMessage(), exception);
                if (modernLoader.getCallback() instanceof AbstractFragmentCallback) {
                    adapter.fragmentFailure(modernLoader.getCallback(), exception);
                } else if (modernLoader.getCallback() instanceof AbstractActivityCallback) {
                    adapter.activityFailure(modernLoader.getCallback(), exception);
                } else {
                    modernLoader.getCallback().onFailure(exception);
                }

            } else {

                if (modernLoader.getCallback() instanceof AbstractFragmentCallback) {
                    adapter.fragmentSuccess(modernLoader.getCallback(), data);
                } else if (modernLoader.getCallback() instanceof AbstractActivityCallback) {
                    adapter.activitySuccess(modernLoader.getCallback(), data);
                } else {
                    modernLoader.getCallback().onSuccess(data);
                }
            }

        }
    }

}
