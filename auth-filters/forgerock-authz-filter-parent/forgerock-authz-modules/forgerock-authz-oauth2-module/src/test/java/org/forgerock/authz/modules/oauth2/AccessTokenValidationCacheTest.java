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
 * Copyright 2014-2015 ForgeRock AS.
 */

package org.forgerock.authz.modules.oauth2;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

public class AccessTokenValidationCacheTest {

    private AccessTokenValidationCache cache;

    @BeforeMethod
    public void setUp() {
        cache = new AccessTokenValidationCache(2);
    }

    @Test
    public void shouldAddEntryToCache() {

        //Given
        String accessToken = "ACCESS_TOKEN";
        AccessTokenValidationResponse validationResponse = new AccessTokenValidationResponse(0);

        //When
        cache.add(accessToken, validationResponse);

        //Then
        assertEquals(cache.size(), 1);
    }

    @Test
    public void shouldReplaceCacheEntry() {

        //Given
        String accessToken = "ACCESS_TOKEN";
        AccessTokenValidationResponse validationResponse = new AccessTokenValidationResponse(0);

        //When
        cache.add(accessToken, validationResponse);
        cache.add(accessToken, validationResponse);

        //Then
        assertEquals(cache.size(), 1);
    }

    @Test
    public void shouldRemoveOldestEntryWhenCacheFull() {

        //Given
        String accessToken1 = "ACCESS_TOKEN_1";
        String accessToken2 = "ACCESS_TOKEN_2";
        String accessToken3 = "ACCESS_TOKEN_3";
        AccessTokenValidationResponse validationResponse1 = new AccessTokenValidationResponse(0);
        AccessTokenValidationResponse validationResponse2 = new AccessTokenValidationResponse(0);
        AccessTokenValidationResponse validationResponse3 = new AccessTokenValidationResponse(0);

        //When
        cache.add(accessToken1, validationResponse1);
        cache.add(accessToken2, validationResponse2);
        cache.add(accessToken3, validationResponse3);

        //Then
        assertNull(cache.get(accessToken1));
        assertEquals(cache.get(accessToken2), validationResponse2);
        assertEquals(cache.get(accessToken3), validationResponse3);
    }

    @Test
    public void shouldGetEntryFromCache() {

        //Given
        String accessToken = "ACCESS_TOKEN";
        AccessTokenValidationResponse validationResponse = new AccessTokenValidationResponse(0);

        //When
        cache.add(accessToken, validationResponse);

        //Then
        assertEquals(cache.get(accessToken), validationResponse);
    }

    @Test
    public void shouldReturnNullWhenAccessTokenNotInCache() {

        //Given
        String accessToken1 = "ACCESS_TOKEN_1";
        String accessToken2 = "ACCESS_TOKEN_2";
        AccessTokenValidationResponse validationResponse = new AccessTokenValidationResponse(0);

        //When
        cache.add(accessToken1, validationResponse);

        //Then
        assertNull(cache.get(accessToken2));
    }

    @Test
    public void cacheShouldBeThreadSafe() throws InterruptedException {

        //Given
        final int runs = 1_000;
        final CountDownLatch latch = new CountDownLatch(runs * 2);
        Runnable reader = new Runnable() {
            @Override
            public void run() {
                cache.get("");
                latch.countDown();
            }
        };
        Runnable writer = new Runnable() {
            @Override
            public void run() {
                cache.add(System.currentTimeMillis() + "", null);
                latch.countDown();
            }
        };

        //When
        for (int i = 0; i < runs; i++) {
            new Thread(writer).start();
        }
        for (int i = 0; i < runs; i++) {
            new Thread(reader).start();
        }

        //Then
        latch.await(10, TimeUnit.MILLISECONDS);
    }
}
