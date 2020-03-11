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
 * Copyright 2015 ForgeRock AS.
 */

package org.forgerock.http.routing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.http.routing.RouteMatchers.*;
import static org.forgerock.http.routing.RoutingMode.STARTS_WITH;
import static org.forgerock.http.routing.Version.version;
import static org.forgerock.util.promise.Promises.newResultPromise;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;

import java.net.URI;

import org.forgerock.services.context.Context;
import org.forgerock.http.Handler;
import org.forgerock.http.session.Session;
import org.forgerock.http.session.SessionContext;
import org.forgerock.http.handler.Handlers;
import org.forgerock.http.header.AcceptApiVersionHeader;
import org.forgerock.http.header.ContentApiVersionHeader;
import org.forgerock.http.header.WarningHeader;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.http.protocol.Status;
import org.forgerock.util.promise.NeverThrowsException;
import org.forgerock.util.promise.Promise;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class RoutingTest {

    private Handler handler;
    private ResourceApiVersionBehaviourManager resourceApiVersionBehaviourManager;

    @BeforeMethod
    public void setup() {
        resourceApiVersionBehaviourManager = newResourceApiVersionBehaviourManager();
        handler = createHandler();
    }

    @DataProvider
    private Object[][] requestData() {
        return new Object[][]{
            {null, null, "2.0", true},
            {null, DefaultVersionBehaviour.NONE, null, true},
            {null, DefaultVersionBehaviour.LATEST, "2.0", true},
            {null, DefaultVersionBehaviour.OLDEST, "1.0", true},
            {"1.0", null, "1.1", false},
            {"1.0", DefaultVersionBehaviour.NONE, "1.1", false},
            {"1.0", DefaultVersionBehaviour.LATEST, "1.1", false},
            {"1.0", DefaultVersionBehaviour.OLDEST, "1.1", false},
            {"1.1", null, "1.1", false},
            {"1.1", DefaultVersionBehaviour.NONE, "1.1", false},
            {"1.1", DefaultVersionBehaviour.LATEST, "1.1", false},
            {"1.1", DefaultVersionBehaviour.OLDEST, "1.1", false},
            {"2.0", null, "2.0", false},
            {"2.0", DefaultVersionBehaviour.NONE, "2.0", false},
            {"2.0", DefaultVersionBehaviour.LATEST, "2.0", false},
            {"2.0", DefaultVersionBehaviour.OLDEST, "2.0", false},
        };
    }

    @Test(dataProvider = "requestData")
    public void shouldCallChfAuthenticateResource(String requestedResourceApiVersion,
            DefaultVersionBehaviour defaultVersionBehaviour, String expectedContentResourceApiVersion,
            boolean isWarningExcepted) {

        //Given
        Context context = mockContext();
        Request request = new Request()
                .setMethod("GET")
                .setUri(URI.create("authenticate"));
        if (requestedResourceApiVersion != null) {
            request.getHeaders().put(AcceptApiVersionHeader.valueOf("resource=" + requestedResourceApiVersion));
        }

        resourceApiVersionBehaviourManager.setDefaultVersionBehaviour(defaultVersionBehaviour);

        //When
        Response response = handler.handle(context, request).getOrThrowUninterruptibly();

        //Then
        if (expectedContentResourceApiVersion != null) {
            assertThat(response.getHeaders().get(ContentApiVersionHeader.NAME)).isNotNull();
            assertThat(response.getHeaders().get(ContentApiVersionHeader.NAME).getValues()).containsOnly("resource="
                    + expectedContentResourceApiVersion);
        } else {
            assertThat(response.getHeaders().get(ContentApiVersionHeader.NAME)).isNull();
        }
        if (isWarningExcepted) {
            assertThat(response.getHeaders().get(WarningHeader.NAME)).isNotNull();
        } else {
            assertThat(response.getHeaders().get(WarningHeader.NAME)).isNull();
        }
    }

    private Context mockContext() {
        return new SessionContext(mock(Context.class), mock(Session.class));
    }

    private Handler createHandler() {
        Router rootRouter = new Router();
        rootRouter.addRoute(requestUriMatcher(STARTS_WITH, "authenticate"), createHttpHandler());
        return rootRouter;
    }

    private Handler createHttpHandler() {
        Router router = new Router();

        router.addRoute(requestResourceApiVersionMatcher(version(1)), mockHandler());
        router.addRoute(requestResourceApiVersionMatcher(version(1, 1)), mockHandler());
        router.addRoute(requestResourceApiVersionMatcher(version(2)), mockHandler());

        return Handlers.chainOf(router, resourceApiVersionContextFilter(resourceApiVersionBehaviourManager));
    }

    private Handler mockHandler() {
        Handler handler = mock(Handler.class);
        Promise<Response, NeverThrowsException> promise = newResultPromise(new Response(Status.OK));
        given(handler.handle(any(Context.class), any(Request.class))).willReturn(promise);
        return handler;
    }
}
