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
package org.jboss.aerogear.android.authentication;

import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.jboss.aerogear.android.Callback;
import org.jboss.aerogear.android.http.HeaderAndBody;

/**
 * This class stubs out the enroll, login, and logout methods. If you call these
 * methods without overriding them they will throw an IllegalStateException in
 * the callback. This will be passed to onFailure as normal.
 */
public abstract class AbstractAuthenticationModule implements
        AuthenticationModule {

    public static final String USERNAME_PARAMETER_NAME = "loginName";
    public static final String PASSWORD_PARAMETER_NAME = "password";
    
    private static final int CORE_POOL_SIZE = 5;
    private static final int MAX_POOL_SIZE = 64;
    private static final int KEEP_ALIVE = 1;
    private static final BlockingQueue<Runnable> WORK_QUEUE =
            new LinkedBlockingQueue<Runnable>(10);

    protected static final Executor THREAD_POOL_EXECUTOR = new ThreadPoolExecutor(CORE_POOL_SIZE, MAX_POOL_SIZE, KEEP_ALIVE,
            TimeUnit.SECONDS, WORK_QUEUE);

    @Override
    public void enroll(Map<String, String> userData,
            final Callback<HeaderAndBody> callback) {
        callback.onFailure(new IllegalStateException("Not implemented"));
    }

    @Override
    public void login(final String username, final String password,
            final Callback<HeaderAndBody> callback) {
        callback.onFailure(new IllegalStateException("Not implemented"));

    }

    @Override
    public void logout(final Callback<Void> callback) {
        callback.onFailure(new IllegalStateException("Not implemented"));
    }

}
