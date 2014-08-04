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

package org.forgerock.json.resource;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.mockito.Mockito.mock;

public class VersionHandlerTest {

    private VersionHandler versionHandler;

    @BeforeMethod
    public void setUp() {

        VersionRouter router = new VersionRouter();
        VersionRouter.VersionRouterImpl versionRouter = new VersionRouter.VersionRouterImpl();
        String uriTemplate = "URI_TEMPLATE";

        versionHandler = new VersionHandler(router, RoutingMode.EQUALS, versionRouter, uriTemplate);
    }

    @Test (expectedExceptions = IllegalArgumentException.class)
    public void addVersionWithRequestHandlerThenCollectionHandlerShouldThrowIllegalArgumentException() {

        //Given
        RequestHandler handler = mock(RequestHandler.class);
        CollectionResourceProvider otherHandler = mock(CollectionResourceProvider.class);

        versionHandler.addVersion("1.0", handler);

        //When
        versionHandler.addVersion("2.0", otherHandler);

        //Then
        //Expected IllegalArgumentException
    }

    @Test (expectedExceptions = IllegalArgumentException.class)
    public void addVersionWithRequestHandlerThenSingletonHandlerShouldThrowIllegalArgumentException() {

        //Given
        RequestHandler handler = mock(RequestHandler.class);
        SingletonResourceProvider otherHandler = mock(SingletonResourceProvider.class);

        versionHandler.addVersion("1.0", handler);

        //When
        versionHandler.addVersion("2.0", otherHandler);

        //Then
        //Expected IllegalArgumentException
    }

    @Test (expectedExceptions = IllegalArgumentException.class)
    public void addVersionWithCollectionHandlerThenRequestHandlerShouldThrowIllegalArgumentException() {

        //Given
        CollectionResourceProvider handler = mock(CollectionResourceProvider.class);
        RequestHandler otherHandler = mock(RequestHandler.class);

        versionHandler.addVersion("1.0", handler);

        //When
        versionHandler.addVersion("2.0", otherHandler);

        //Then
        //Expected IllegalArgumentException
    }

    @Test (expectedExceptions = IllegalArgumentException.class)
    public void addVersionWithCollectionHandlerThenSingletonHandlerShouldThrowIllegalArgumentException() {

        //Given
        CollectionResourceProvider handler = mock(CollectionResourceProvider.class);
        SingletonResourceProvider otherHandler = mock(SingletonResourceProvider.class);

        versionHandler.addVersion("1.0", handler);

        //When
        versionHandler.addVersion("2.0", otherHandler);

        //Then
        //Expected IllegalArgumentException
    }

    @Test (expectedExceptions = IllegalArgumentException.class)
    public void addVersionWithSingletonHandlerThenRequestHandlerShouldThrowIllegalArgumentException() {

        //Given
        SingletonResourceProvider handler = mock(SingletonResourceProvider.class);
        RequestHandler otherHandler = mock(RequestHandler.class);

        versionHandler.addVersion("1.0", handler);

        //When
        versionHandler.addVersion("2.0", otherHandler);

        //Then
        //Expected IllegalArgumentException
    }

    @Test (expectedExceptions = IllegalArgumentException.class)
    public void addVersionWithSingletonHandlerThenCollectionHandlerShouldThrowIllegalArgumentException() {

        //Given
        SingletonResourceProvider handler = mock(SingletonResourceProvider.class);
        CollectionResourceProvider otherHandler = mock(CollectionResourceProvider.class);

        versionHandler.addVersion("1.0", handler);

        //When
        versionHandler.addVersion("2.0", otherHandler);

        //Then
        //Expected IllegalArgumentException
    }
}
