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
import org.jboss.aerogear.android.Callback;
import org.jboss.aerogear.android.authentication.AbstractAuthenticationModule;
import org.jboss.aerogear.android.authentication.AuthenticationModule;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.concurrent.CountDownLatch;

import static org.mockito.Mockito.*;

@RunWith(RobolectricTestRunner.class)
public class GeneralAuthenticationModuleTest implements AuthenticationModuleTest {

    private static final URL SIMPLE_URL;

    static {
        try {
            SIMPLE_URL = new URL("http://localhost:8080/todo-server");
        } catch (MalformedURLException ex) {
            throw new IllegalStateException(ex);
        }
    }

    @Test(timeout = 1000l)
    public void testAbstractMethodsThrowExceptions() throws InterruptedException {
        AuthenticationModule module = mock(AbstractAuthenticationModule.class, CALLS_REAL_METHODS);
        final CountDownLatch latch = new CountDownLatch(3);
        Callback throwIfSuccess = new Callback() {

            @Override
            public void onSuccess(Object data) {
                Assert.assertTrue("This should not be called", false);
            }

            @Override
            public void onFailure(Exception e) {
                latch.countDown();
            }
        };
        module.enroll(new HashMap<String, String>(), throwIfSuccess);
        module.login("username", "password", throwIfSuccess);
        module.logout(throwIfSuccess);

        latch.await();

    }

}
