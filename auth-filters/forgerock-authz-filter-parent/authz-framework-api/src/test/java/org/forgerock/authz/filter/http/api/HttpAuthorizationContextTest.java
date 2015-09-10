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

package org.forgerock.authz.filter.http.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.forgerock.authz.filter.api.AuthorizationContext;
import org.forgerock.http.context.AttributesContext;
import org.forgerock.http.context.RootContext;
import org.testng.annotations.Test;

/**
 * Unit tests for {@link org.forgerock.authz.filter.api.AuthorizationContext}.
 *
 * @since 1.4.0
 */
public class HttpAuthorizationContextTest {

    @Test
    public void shouldPropagateUpdatesToTheRequestMap() {

        // Given
        AttributesContext requestContext = new AttributesContext(new RootContext());
        Map<String, Object> contextMap = new LinkedHashMap<>();
        requestContext.getAttributes().put(HttpAuthorizationContext.ATTRIBUTE_AUTHORIZATION_CONTEXT, contextMap);

        // When
        AuthorizationContext context = HttpAuthorizationContext.forRequest(requestContext);
        context.setAttribute("one", 2);

        // Then the map in the request should also be updated
        assertThat(contextMap).isEqualTo(Collections.singletonMap("one", 2));
    }

    @Test
    public void shouldCreateContextMapOnRequest() {

        // Given
        AttributesContext requestContext = new AttributesContext(new RootContext());
        requestContext.getAttributes().put(HttpAuthorizationContext.ATTRIBUTE_AUTHORIZATION_CONTEXT, null);

        // When
        HttpAuthorizationContext.forRequest(requestContext);

        // Then
        assertThat(requestContext.getAttributes().get(HttpAuthorizationContext.ATTRIBUTE_AUTHORIZATION_CONTEXT))
                .isInstanceOf(Map.class);
    }

    @Test
    public void shouldThrowClassCaseExceptionWhenContextNotMap() {

        // Given
        AttributesContext requestContext = new AttributesContext(new RootContext());
        requestContext.getAttributes().put(HttpAuthorizationContext.ATTRIBUTE_AUTHORIZATION_CONTEXT, "STRING");

        // When
        HttpAuthorizationContext.forRequest(requestContext);

        // Then
        assertThat(requestContext.getAttributes().get(HttpAuthorizationContext.ATTRIBUTE_AUTHORIZATION_CONTEXT))
                .isInstanceOf(Map.class);
    }
}
