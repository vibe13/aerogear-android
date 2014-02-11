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
package org.jboss.aerogear.android.impl.pipeline;

import com.xtremelabs.robolectric.RobolectricTestRunner;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jboss.aerogear.android.authentication.AuthenticationModule;
import org.jboss.aerogear.android.http.HttpException;
import org.jboss.aerogear.android.http.HttpProvider;
import org.jboss.aerogear.android.impl.helper.Data;
import org.jboss.aerogear.android.impl.helper.UnitTestUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.mockito.Mockito.*;

@RunWith(RobolectricTestRunner.class)
public class RestRunnerTest {
    private static final URL SIMPLE_URL;

    static {
        try {
            SIMPLE_URL = new URL("http://example.com");
        } catch (MalformedURLException ex) {
            Logger.getLogger(RestRunnerTest.class.getName()).log(Level.SEVERE, null, ex);
            throw new RuntimeException(ex);
        }
    }

    @Test
    public void testRestRunnerRetriesAuthWith401() {
        RestRunner<Data> runner = new RestRunner<Data>(Data.class, SIMPLE_URL);
        AuthenticationModule mockModule = createMockModule();
        runner.setAuthenticationModule(mockModule);

        HttpProvider mockProvider = createMockProvider(401);
        try {
            UnitTestUtils.callPrivateMethod(runner, "getResponse", new Class[] { HttpProvider.class }, new Object[] { mockProvider });
        } catch (Exception ignore) {
        }

        verify(mockModule, times(1)).retryLogin();
        verify(mockProvider, times(2)).get();

    }

    @Test
    public void testRestRunnerRetriesAuthWith403() {
        RestRunner<Data> runner = new RestRunner<Data>(Data.class, SIMPLE_URL);
        AuthenticationModule mockModule = createMockModule();
        runner.setAuthenticationModule(mockModule);

        HttpProvider mockProvider = createMockProvider(403);
        try {
            UnitTestUtils.callPrivateMethod(runner, "getResponse", new Class[] { HttpProvider.class }, new Object[] { mockProvider });
        } catch (Exception ignore) {
        }

        verify(mockModule, times(1)).retryLogin();
        verify(mockProvider, times(2)).get();
    }

    private AuthenticationModule createMockModule() {
        AuthenticationModule module = mock(AuthenticationModule.class);
        when(module.isLoggedIn()).thenReturn(true);
        when(module.retryLogin()).thenReturn(Boolean.TRUE);
        return module;
    }

    private HttpProvider createMockProvider(int statusCode) {
        HttpProvider provider = mock(HttpProvider.class);
        when(provider.get()).thenThrow(new HttpException(new byte[0], statusCode));
        return provider;
    }

}
