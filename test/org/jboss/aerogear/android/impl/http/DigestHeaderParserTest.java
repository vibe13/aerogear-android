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
package org.jboss.aerogear.android.impl.http;

import com.xtremelabs.robolectric.RobolectricTestRunner;
import java.util.Map;
import org.jboss.aerogear.android.authentication.impl.DigestHeaderUtils;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.runner.RunWith;

@RunWith(RobolectricTestRunner.class)
public class DigestHeaderParserTest {

    private static final String PASSING_HEADER_SINGLE_QOP = " Digest realm=\"default\",domain=\"/aerogear-controller-demo\",nonce=\"MTM3MjQ0MTQzNDE5MTozMzRhMWY1NC1mNWE1LTQ4Y2EtODkyYi01NWJjMWM4ZWIwM2Y=\",algorithm=MD5,qop=auth,stale=\"false\"";
    private static final String PASSING_HEADER_MULTI_QOP = " Digest realm=\"default\",domain=\"/aerogear-controller-demo\",nonce=\"MTM3MjQ0MTQzNDE5MTozMzRhMWY1NC1mNWE1LTQ4Y2EtODkyYi01NWJjMWM4ZWIwM2Y=\",algorithm=MD5,qop=\"auth,auth-int\",stale=\"false\"";
    private static final String FAILING_HEADER = "NotDigest values=\"someValue\"";
    private static final String REALM = "realm";
    private static final String DOMAIN = "domain";
    private static final String NONCE = "nonce";
    private static final String STALE = "stale";
    private static final String ALGORITHM = "algorithm";
    private static final String QOP_OPTIONS = "qop";

    @Test
    public void testSingleHeaderPasses() {
        Map<String, String> values = DigestHeaderUtils.extractValues(PASSING_HEADER_SINGLE_QOP);
        assertEquals("default", values.get(REALM));
        assertEquals("/aerogear-controller-demo", values.get(DOMAIN));
        assertEquals("MTM3MjQ0MTQzNDE5MTozMzRhMWY1NC1mNWE1LTQ4Y2EtODkyYi01NWJjMWM4ZWIwM2Y=", values.get(NONCE));
        assertEquals("MD5", values.get(ALGORITHM));
        assertEquals("auth", values.get(QOP_OPTIONS));
        assertEquals("false", values.get(STALE));
    }

    @Test
    public void testMultiHeaderPasses() {
        Map<String, String> values = DigestHeaderUtils.extractValues(PASSING_HEADER_MULTI_QOP);
        assertEquals("default", values.get(REALM));
        assertEquals("/aerogear-controller-demo", values.get(DOMAIN));
        assertEquals("MTM3MjQ0MTQzNDE5MTozMzRhMWY1NC1mNWE1LTQ4Y2EtODkyYi01NWJjMWM4ZWIwM2Y=", values.get(NONCE));
        assertEquals("MD5", values.get(ALGORITHM));
        assertEquals("auth,auth-int", values.get(QOP_OPTIONS));
        assertEquals("false", values.get(STALE));
    }

    @Test
    public void testHeaderFails() {
        try {
            DigestHeaderUtils.extractValues(FAILING_HEADER);
            fail();
        } catch (IllegalArgumentException ex) {
            assertEquals(FAILING_HEADER + " Did not begin with the Digest challenge string.", ex.getMessage());
        }
    }
}
