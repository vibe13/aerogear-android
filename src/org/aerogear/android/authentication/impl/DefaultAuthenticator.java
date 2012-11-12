/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012, Red Hat, Inc., and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.aerogear.android.authentication.impl;

import java.net.MalformedURLException;
import org.aerogear.android.authentication.AuthenticationModule;
import org.aerogear.android.authentication.Authenticator;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import org.aerogear.android.authentication.AuthenticationConfig;
import org.aerogear.android.authentication.impl.AuthTypes;

/**
 * This is the default implementation of Authenticator.
 * It uses a HashMap behind the scenes to store its modules.
 */
public class DefaultAuthenticator implements Authenticator {

    private Map<String, AuthenticationModule> modules = new HashMap<String, AuthenticationModule>();
    private final URL baseURL;

    public DefaultAuthenticator(URL baseURL) {
        this.baseURL = baseURL;
    }
    
    public DefaultAuthenticator(String baseURL) {
        try {
            this.baseURL = new URL(baseURL);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(e);
        }
    }


    /**
     * {@inheritDoc }
     */
    @Override
    public AuthenticationModule get(String name) {
        return modules.get(name);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public AuthenticationModule remove(String name) {
        return modules.remove(name);
    }

      /**
     * {@inheritDoc }
     */
    @Override
    public AuthenticationModule auth(String name, AuthenticationConfig config) {
        
        assert config != null;
        
        if (!AuthTypes.REST.equals(config.getAuthType())) {
            throw new IllegalArgumentException("Unsupported Auth Type passed");
        }
        modules.put(name, new RestAuthenticationModule(baseURL, config));
        return modules.get(name);
        
    }
    
}