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
package org.jboss.aerogear.android.impl.util;

import org.jboss.aerogear.android.impl.reflection.Property;
import org.jboss.aerogear.crypto.CryptoBox;
import org.jboss.aerogear.crypto.keys.PrivateKey;

import java.lang.reflect.Field;
import java.util.*;

public class CryptoUtils<T> {

    private final CryptoBox cryptoBox;
    private final byte[] IV;
    private final Class<T> modelClass;

    public CryptoUtils(PrivateKey privateKey, byte[] iv, Class<T> modelClass) {
        this.modelClass = modelClass;
        this.cryptoBox = new CryptoBox(privateKey);
        this.IV = iv;
    }

    public Collection<T> decrypt(Collection<Map<String, byte[]>> encryptedCollection) {
        List<T> decryptedList = new ArrayList<T>();
        for (Map<String, byte[]> encryptedItem : encryptedCollection) {
            decryptedList.add(decrypt(encryptedItem));
        }
        return decryptedList;
    }

    public T decrypt(Map<String, byte[]> encryptedItem) {
        T decryptedItem = newObject();
        for (String fieldName : encryptedItem.keySet()) {
            if (!fieldName.equals("id")) {
                byte[] encryptedData = encryptedItem.get(fieldName);
                byte[] decryptedData = d(encryptedData);
                Property property = new Property(decryptedItem.getClass(), fieldName);
                property.setValue(decryptedItem, new String(decryptedData));
            }
        }
        return decryptedItem;
    }

    public Map<String, byte[]> encrypt(T item) {
        Map<String, byte[]> encryptedValues = new HashMap<String, byte[]>();

        Field[] declaredFields = item.getClass().getDeclaredFields();
        for (Field field : declaredFields) {
            Property property = new Property(item.getClass(), field.getName());
            Object decryptedValue = property.getValue(item);

            if (decryptedValue != null) {
                encryptedValues.put(field.getName(), e(decryptedValue));
            }
        }

        return encryptedValues;
    }

    private T newObject() {
        try {
            return modelClass.newInstance();
        } catch (InstantiationException e) {
            throw new RuntimeException("An error occurred while trying to build the object. " +
                    "Make sure it has a default constructor");
        } catch (IllegalAccessException e) {
            throw new RuntimeException("An error occurred while trying to build the object. " +
                    "Make sure it has a default constructor");
        }
    }

    private byte[] e(Object data) {
        final byte[] message = data.toString().getBytes();
        return cryptoBox.encrypt(IV, message);
    }

    private byte[] d(byte[] data) {
        return cryptoBox.decrypt(IV, data);
    }

}
