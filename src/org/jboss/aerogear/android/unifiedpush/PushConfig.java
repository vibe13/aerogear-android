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
package org.jboss.aerogear.android.unifiedpush;

import java.io.Serializable;

import com.google.common.collect.ImmutableSet;

import java.net.URI;
import java.util.List;

import org.jboss.aerogear.android.impl.unifiedpush.PushTypes;

public class PushConfig implements Serializable {

    private static final long serialVersionUID = 1L;
    private String deviceToken;
    private String variantID;
    private String secret;
    private String deviceType = "ANDROID";
    private String operatingSystem = "android";
    private String osVersion = android.os.Build.VERSION.RELEASE;
    private String alias;
    private List<String> categories;
    private PushType type = PushTypes.AEROGEAR_GCM;
    private URI pushServerURI;

    public final ImmutableSet<String> senderIds;

    public PushConfig(URI pushServerURI, String... senderId) {
        senderIds = ImmutableSet.copyOf(senderId);
        this.pushServerURI = pushServerURI;
    }

    public PushConfig(String... senderId) {
        senderIds = ImmutableSet.copyOf(senderId);
    }

    /**
     * The device token Identifies the device within its Push Network. It is the
     * value = GoogleCloudMessaging.getInstance(context).register(SENDER_ID);
     */
    public String getDeviceToken() {
        return deviceToken;
    }

    /**
     * The device token Identifies the device within its Push Network. It is the
     * value = GoogleCloudMessaging.getInstance(context).register(SENDER_ID);
     */
    public void setDeviceToken(String deviceToken) {
        this.deviceToken = deviceToken;
    }

    /**
     * ID of the Variant from the AeroGear UnifiedPush Server.
     */
    public String getVariantID() {
        return variantID;
    }

    /**
     * ID of the Variant from the AeroGear UnifiedPush Server.
     */
    public void setVariantID(String variantID) {
        this.variantID = variantID;
    }

    /**
     * Secret of the Variant from the AeroGear UnifiedPush Server.     
     */
    public String getSecret() {
        return secret;
    }

    /**
     * Secret of the Variant from the AeroGear UnifiedPush Server.
     */
    public void setSecret(String secret) {
        this.secret = secret;
    }

    /**
     * Device type determines which cloud messaging system will be used by the
     * AeroGear Unified Push Server
     *
     * Defaults to ANDROID
     */
    public String getDeviceType() {
        return deviceType;
    }

    /**
     * Device type determines which cloud messaging system will be used by the
     * AeroGear Unified Push Server.
     *
     * Defaults to ANDROID
     */
    public void setDeviceType(String deviceType) {
        this.deviceType = deviceType;
    }

    /**
     * The name of the operating system. Defaults to Android
     */
    public String getOperatingSystem() {
        return operatingSystem;
    }

    /**
     * The name of the operating system. Defaults to Android
     */
    public void setOperatingSystem(String operatingSystem) {
        this.operatingSystem = operatingSystem;
    }

    /**
     * The version of the operating system running.
     *
     * Defaults to the value provided by android.os.Build.VERSION.RELEASE
     */
    public String getOsVersion() {
        return osVersion;
    }

    /**
     * The version of the operating system running.
     *
     * Defaults to the value provided by android.os.Build.VERSION.RELEASE
     */
    public void setOsVersion(String osVersion) {
        this.osVersion = osVersion;
    }

    /**
     * The Alias is an identifier of the user of the system.
     *
     * Examples are an email address or a username
     *
     */
    public String getAlias() {
        return alias;
    }

    /**
     * The Alias is an identifier of the user of the system.
     *
     * Examples are an email address or a username
     *
     */
    public void setAlias(String alias) {
        this.alias = alias;
    }

    /**
     * The categories specifies a channel which may be used to send messages
     */
    public List<String> getCategories() {
        return categories;
    }

    /**
     * The categories specifies a channel which may be used to send messages
     */
    public void setCategories(List<String> categories) {
        this.categories = categories;
    }

    /**
     * The type is a key which is used by Registrations to know which 
     * implementation of PushRegister to use.
     * 
     * @return the current type
     */
    public PushType getType() {
        return type;
    }

    /**
     * The type is a key which is used by Registrations to know which 
     * implementation of PushRegister to use.
     */
    public void setType(PushType type) {
        this.type = type;
    }

    /**
     * RegistryURL is the URL of the 3rd party application server
     *
     * @return the current pushServerURI
     */
    public URI getPushServerURI() {
        return pushServerURI;
    }

    /**
     * RegistryURL is the URL of the 3rd party application server
     *
     */
    public void setPushServerURI(URI pushServerURI) {
        this.pushServerURI = pushServerURI;
    }

}
