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

import java.net.URI;
import java.util.Collections;
import java.util.List;

import org.forgerock.services.context.Context;
import org.forgerock.services.context.RootContext;
import org.forgerock.http.protocol.Request;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class RouteMatchersTest {

    private Request request;

    @BeforeClass
    public void setup() {
        request = new Request().setUri(URI.create("http://example.com:8080/json/users/demo"));
    }

    @DataProvider
    private Object[][] testData() {
        Context parentContext = new RootContext();
        return new Object[][]{
            {parentContext, "json/users/demo"},
            {newContext(parentContext, ""), "json/users/demo"},
            {newContext(parentContext, "json"), "users/demo"},
            {newContext(newContext(parentContext, "json"), "users"), "demo"},
            {newContext(newContext(newContext(parentContext, "json"), "users"), "demo"), ""},
        };
    }

    @Test(dataProvider = "testData")
    public void shouldGetUri(Context context, String expectedUri) {

        //When
        List<String> uri = RouteMatchers.getRemainingRequestUri(context, request);

        //Then
        if (expectedUri.isEmpty()) {
            assertThat(uri).isEmpty();
        } else {
            assertThat(uri).containsExactly(expectedUri.split("/"));
        }
    }

    private UriRouterContext newContext(Context parentContext, String matchedUri) {
        return new UriRouterContext(parentContext, matchedUri, "REMAINING", Collections.<String, String>emptyMap());
    }
}
