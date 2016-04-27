/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2014 ForgeRock AS.
 */

package org.forgerock.authz.modules.oauth2;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * <p>Cache for caching AccessToken validation results.</p>
 *
 * <p>The cache is created with a maximum size, which when reached the cache will remove the oldest entry.</p>
 *
 * @since 1.4.0
 */
class AccessTokenValidationCache {

    private final Map<String, AccessTokenValidationResponse> cache;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    /**
     * Constructs a new instance of the AccessTokenValidationCache.
     *
     * @param maxSize The maximum size of the cache.
     */
    AccessTokenValidationCache(final int maxSize) {
        cache = new LinkedHashMap<String, AccessTokenValidationResponse>(maxSize) {
            /**
             * Serial Version UID.
             */
            public static final long serialVersionUID = -1L;

            @Override
            protected boolean removeEldestEntry(Map.Entry<String, AccessTokenValidationResponse> eldestEntry) {
                return size() > maxSize;
            }
        };
    }

    /**
     * Adds an entry to the cache.
     *
     * @param accessToken The access token.
     * @param validationResponse The validation response.
     */
    void add(String accessToken, AccessTokenValidationResponse validationResponse) {
        try {
            lock.writeLock().lock();
            cache.put(accessToken, validationResponse);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Retrieves an entry from the cache.
     *
     * @param accessToken The access token.
     * @return The access tokens validation response.
     */
    AccessTokenValidationResponse get(String accessToken) {
        try {
            lock.readLock().lock();
            return cache.get(accessToken);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Returns the current size of the cache.
     *
     * @return The cache size.
     */
    int size() {
        try {
            lock.readLock().lock();
            return cache.size();
        } finally {
            lock.readLock().unlock();
        }
    }
}
