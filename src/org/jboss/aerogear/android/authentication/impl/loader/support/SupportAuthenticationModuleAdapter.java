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
package org.jboss.aerogear.android.authentication.impl.loader.support;


import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.jboss.aerogear.android.Callback;
import org.jboss.aerogear.android.authentication.AuthenticationModule;
import org.jboss.aerogear.android.authentication.AuthorizationFields;
import org.jboss.aerogear.android.authentication.impl.loader.AuthenticationModuleAdapter;
import org.jboss.aerogear.android.authentication.impl.loader.LoaderAuthenticationModule;
import org.jboss.aerogear.android.http.HeaderAndBody;
import org.jboss.aerogear.android.pipeline.support.AbstractFragmentActivityCallback;
import org.jboss.aerogear.android.pipeline.support.AbstractSupportFragmentCallback;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.util.Log;

import com.google.common.base.Objects;
import java.net.URI;
import org.jboss.aerogear.android.authentication.AbstractAuthenticationModule;
import static org.jboss.aerogear.android.authentication.impl.loader.LoaderAuthenticationModule.CALLBACK;
import static org.jboss.aerogear.android.authentication.impl.loader.LoaderAuthenticationModule.METHOD;
import static org.jboss.aerogear.android.authentication.impl.loader.LoaderAuthenticationModule.PARAMS;

/**
 * This class manages the relationship between Android's Loader framework and
 * requests to Authentication. This class acts as a proxy for an
 * {@link AuthenticationModule} instance.
 *
 * This class uses the Android support versions of the Loader API. If you do not
 * need to support devices &lt; Android 3.0, see
 * {@link AuthenticationModuleAdapter}
 */
public class SupportAuthenticationModuleAdapter implements LoaderAuthenticationModule, LoaderManager.LoaderCallbacks<HeaderAndBody> {

    private static final String TAG = SupportAuthenticationModuleAdapter.class.getSimpleName();

    public static enum Methods {

        LOGIN, LOGOUT, ENROLL
    };

    private final Context applicationContext;
    private final AuthenticationModule module;
    private final LoaderManager manager;
    private final FragmentActivity activity;
    private final Fragment fragment;
    private final Handler handler;
    private final String name;

    public SupportAuthenticationModuleAdapter(FragmentActivity activity, AuthenticationModule module, String name) {
        this.module = module;
        this.manager = activity.getSupportLoaderManager();
        this.applicationContext = activity.getApplicationContext();
        this.fragment = null;
        this.activity = activity;
        this.handler = new Handler(Looper.getMainLooper());
        this.name = name;
    }

    public SupportAuthenticationModuleAdapter(Fragment fragment, Context applicationContext, AuthenticationModule module, String name) {
        this.module = module;
        this.manager = fragment.getLoaderManager();
        this.applicationContext = applicationContext;
        this.fragment = fragment;
        this.activity = null;
        this.handler = new Handler(Looper.getMainLooper());
        this.name = name;
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
        bundle.putSerializable(METHOD, SupportAuthenticationModuleAdapter.Methods.ENROLL);
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
        int id = Objects.hashCode(name, loginData, callback, Math.random());
        Bundle bundle = new Bundle();
        Bundle loginBundle = new Bundle();
        
        for (Map.Entry<String, String> entry : loginData.entrySet()) {
            loginBundle.putString(entry.getKey(), entry.getValue());
        }
        
        bundle.putSerializable(CALLBACK, callback);
        bundle.putBundle(PARAMS, loginBundle);
        bundle.putSerializable(METHOD, Methods.LOGIN);
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
        bundle.putSerializable(METHOD, SupportAuthenticationModuleAdapter.Methods.LOGOUT);
        manager.initLoader(id, bundle, this);
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
        SupportAuthenticationModuleAdapter.Methods method = (SupportAuthenticationModuleAdapter.Methods) bundle.get(METHOD);
        Callback callback = (Callback) bundle.get(CALLBACK);
        Loader loader = null;
        switch (method) {
        case LOGIN: {
            Bundle loginBundle = bundle.getBundle(PARAMS);
            Map<String, String> loginParams = new HashMap<String, String>(loginBundle.size());
            for (String key : loginBundle.keySet()) {
                loginParams.put(key, loginBundle.getString(key));
            }
            loader = new SupportLoginLoader(applicationContext, callback, module, loginParams);
        }
            break;
        case LOGOUT: {
            loader = new SupportLogoutLoader(applicationContext, callback, module);
        }
            break;
        case ENROLL: {
            Map<String, String> params = (Map<String, String>) bundle.getSerializable(PARAMS);
            loader = new SupportEnrollLoader(applicationContext, callback, module, params);
        }
            break;
        }
        return loader;
    }

    /**
     * This method will call the Callback for a enroll, login, or logout method
     * on the main thread of the application. If a callback is an instance of
     * {@link AbstractSupportFragmentCallback} or
     * {@link AbstractFragmentActivityCallback} then it will also configure the
     * reference to {@link Fragment} or {@link FragmentActivity} for the
     * callback.
     */
    @Override
    public void onLoadFinished(Loader<HeaderAndBody> loader, final HeaderAndBody data) {
        if (!(loader instanceof AbstractSupportAuthenticationLoader)) {
            Log.e(TAG, "Adapter is listening to loaders which it doesn't support");
            throw new IllegalStateException("Adapter is listening to loaders which it doesn't support");
        } else {
            final AbstractSupportAuthenticationLoader supportLoader = (AbstractSupportAuthenticationLoader) loader;
            handler.post(new CallbackHandler(this, supportLoader, data));
        }
    }

    @Override
    public void onLoaderReset(Loader<HeaderAndBody> loader) {
        //Do nothing, should call logout on module manually.
    }

    private void fragmentSuccess(Callback<HeaderAndBody> typelessCallback, HeaderAndBody data) {
        AbstractSupportFragmentCallback callback = (AbstractSupportFragmentCallback) typelessCallback;
        callback.setFragment(fragment);
        callback.onSuccess(data);
        callback.setFragment(null);
    }

    private void fragmentFailure(Callback<HeaderAndBody> typelessCallback, Exception exception) {
        AbstractSupportFragmentCallback callback = (AbstractSupportFragmentCallback) typelessCallback;
        callback.setFragment(fragment);
        callback.onFailure(exception);
        callback.setFragment(null);
    }

    private void activitySuccess(Callback<HeaderAndBody> typelessCallback, HeaderAndBody data) {
        AbstractFragmentActivityCallback callback = (AbstractFragmentActivityCallback) typelessCallback;
        callback.setFragmentActivity(activity);
        callback.onSuccess(data);
        callback.setFragmentActivity(null);
    }

    private void activityFailure(Callback<HeaderAndBody> typelessCallback, Exception exception) {
        AbstractFragmentActivityCallback callback = (AbstractFragmentActivityCallback) typelessCallback;
        callback.setFragmentActivity(activity);
        callback.onFailure(exception);
        callback.setFragmentActivity(null);
    }

    final static class CallbackHandler implements Runnable {

        private final SupportAuthenticationModuleAdapter adapter;
        private final AbstractSupportAuthenticationLoader supportLoader;
        private final HeaderAndBody data;

        public CallbackHandler(SupportAuthenticationModuleAdapter adapter,
                AbstractSupportAuthenticationLoader loader, HeaderAndBody data) {
            super();
            this.adapter = adapter;
            this.supportLoader = loader;
            this.data = data;
        }

        @Override
        public void run() {
            if (supportLoader.hasException()) {
                final Exception exception = supportLoader.getException();
                Log.e(TAG, exception.getMessage(), exception);
                if (supportLoader.getCallback() instanceof AbstractSupportFragmentCallback) {
                    adapter.fragmentFailure(supportLoader.getCallback(), exception);
                } else if (supportLoader.getCallback() instanceof AbstractFragmentActivityCallback) {
                    adapter.activityFailure(supportLoader.getCallback(), exception);
                } else {
                    supportLoader.getCallback().onFailure(exception);
                }

            } else {

                if (supportLoader.getCallback() instanceof AbstractSupportFragmentCallback) {
                    adapter.fragmentSuccess(supportLoader.getCallback(), data);
                } else if (supportLoader.getCallback() instanceof AbstractFragmentActivityCallback) {
                    adapter.activitySuccess(supportLoader.getCallback(), data);
                } else {
                    supportLoader.getCallback().onSuccess(data);
                }
            }

        }
    }
}
