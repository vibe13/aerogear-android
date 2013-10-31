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
package org.jboss.aerogear.android.impl.datamanager;

import org.jboss.aerogear.android.ReadFilter;
import org.jboss.aerogear.android.datamanager.IdGenerator;
import org.jboss.aerogear.android.datamanager.Store;
import org.jboss.aerogear.android.datamanager.StoreType;
import org.jboss.aerogear.android.impl.crypto.InvalidKeyException;
import org.jboss.aerogear.android.impl.util.CryptoUtils;
import org.jboss.aerogear.crypto.Random;
import org.jboss.aerogear.crypto.keys.PrivateKey;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class EncryptedMemoryStore<T> implements Store<T> {

    private final MemoryStorage<Map<String, byte[]>> memoryStorage;
    private final CryptoUtils<T> cryptoUtils;

    public EncryptedMemoryStore(IdGenerator idGenerator, PrivateKey privateKey, Class<T> modelClass) {
        memoryStorage = new MemoryStorage(idGenerator);
        byte[] IV = new Random().randomBytes();
        cryptoUtils = new CryptoUtils<T>(privateKey, IV, modelClass);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public StoreType getType() {
        return StoreTypes.ENCRYPTED_MEMORY;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<T> readAll() throws InvalidKeyException {
        Collection<Map<String, byte[]>> encryptedCollection = memoryStorage.readAll();
        return cryptoUtils.decrypt(encryptedCollection);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public T read(Serializable id) throws InvalidKeyException {
        Map<String, byte[]> encryptedItem = memoryStorage.read(id);
        return cryptoUtils.decrypt(encryptedItem);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<T> readWithFilter(ReadFilter filter) throws InvalidKeyException {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void save(T item) {
        Serializable idValue = memoryStorage.getOrGenerateIdValue(item);
        memoryStorage.save(idValue, cryptoUtils.encrypt(item));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void reset() {
        memoryStorage.reset();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void remove(Serializable id) {
        memoryStorage.remove(id);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isEmpty() {
        return memoryStorage.isEmpty();
    }

}
