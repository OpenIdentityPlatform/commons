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
import static org.mockito.Mockito.mock;

import java.util.Collections;

import org.forgerock.http.Context;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class RouterContextTest {

    @DataProvider
    private Object[][] testData() {
        Context parentContext = mock(Context.class);
        return new Object[][]{
            {newContext(parentContext, "MATCHED_URI"), "MATCHED_URI"},
            {newContext(newContext(parentContext, "PREVIOUSLY_MATCHED_URI"), "MATCHED_URI"),
                "PREVIOUSLY_MATCHED_URI/MATCHED_URI"},
            {newContext(newContext(
                newContext(parentContext, "FIRST_MATCHED_URI"), "PREVIOUSLY_MATCHED_URI"), "MATCHED_URI"),
                "FIRST_MATCHED_URI/PREVIOUSLY_MATCHED_URI/MATCHED_URI"},
            {newContext(newContext(parentContext, "PREVIOUSLY_MATCHED_URI"), ""), "PREVIOUSLY_MATCHED_URI"},

        };
    }

    @Test(dataProvider = "testData")
    public void shouldGetBaseUri(RouterContext context, String expectedBaseUri) {

        //When
        String baseUri = context.getBaseUri();

        //Then
        assertThat(baseUri).isEqualTo(expectedBaseUri);
    }

    private RouterContext newContext(Context parentContext, String matchedUri) {
        return new RouterContext(parentContext, matchedUri, "REMAINING", Collections.<String, String>emptyMap());
    }
}
