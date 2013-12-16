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

import android.util.Log;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

public final class DigestHeaderUtils {

    private static final String TAG = DigestHeaderUtils.class.getSimpleName();

    private static enum States {

        DIGEST, KEY, VALUE
    };

    private static final String DIGEST = "Digest";
    private static final String WHITESPACE = "\\s";
    private static final String COMMA = ",";
    private static final String QUOTE = "\"";
    private static final String EQ = "=";

    public static Map<String, String> extractValues(String authenticateHeader) {
        States state = States.DIGEST;
        StringBuilder word = new StringBuilder();
        String key = "";
        String value = "";
        String valueTerminator = COMMA;

        Map<String, String> values = new HashMap<String, String>();
        authenticateHeader = authenticateHeader.trim();
        if (!authenticateHeader.startsWith(DIGEST)) {
            throw new IllegalArgumentException(authenticateHeader + " Did not begin with the Digest challenge string.");
        }

        for (Character character : authenticateHeader.toCharArray()) {
            if (matches(character, WHITESPACE)) {
                continue;
            }
            switch (state) {
            case DIGEST:
                word.append(character);
                if (word.lastIndexOf(DIGEST) != -1) {
                    word = new StringBuilder();
                    state = States.KEY;
                }
                break;
            case KEY:
                if (matches(character, EQ)) {
                    key = word.toString();
                    word = new StringBuilder();
                    state = States.VALUE;
                    break;
                } else if (matches(character, COMMA)) {
                    break;
                } else {
                    word.append(character);
                }
                break;
            case VALUE:
                if (matches(character, valueTerminator)) {
                    value = word.toString();
                    word = new StringBuilder();
                    valueTerminator = COMMA;
                    values.put(key, value);
                    state = States.KEY;
                    break;
                } else {
                    if (matches(character, QUOTE)) {
                        valueTerminator = QUOTE;
                        break;
                    } else {
                        word.append(character);
                    }
                }
                break;
            }
        }

        return values;

    }

    private static boolean matches(Character character, String regex) {
        String str = character.toString();
        return str.matches(regex);
    }

    public static String computeMD5Hash(byte[] bytes) {

        try {
            // Create MD5 Hash
            MessageDigest digest = java.security.MessageDigest.getInstance("MD5");
            digest.update(bytes);
            byte messageDigest[] = digest.digest();

            StringBuilder md5Hash = new StringBuilder();
            for (int i = 0; i < messageDigest.length; i++) {
                String hexChar = Integer.toHexString(0xFF & messageDigest[i]);
                while (hexChar.length() < 2) {
                    hexChar = "0" + hexChar;
                }
                md5Hash.append(hexChar);
            }

            return md5Hash.toString();

        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, e.getMessage(), e);
            throw new RuntimeException(e);
        }

    }
}
