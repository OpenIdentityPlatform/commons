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
 * information: "Portions Copyright [year] [name of copyright owner]".
 *
 * Copyright 2014-2016 ForgeRock AS.
 */

package org.forgerock.http.protocol;

import static org.assertj.core.api.Assertions.assertThat;

import org.testng.annotations.Test;

@SuppressWarnings("javadoc")
public class ResponseTest {
    @Test
    public void testMethodChaining() {
        Response response = new Response().setVersion("123").setStatus(Status.OK);
        assertThat(response.getVersion()).isEqualTo("123");
        assertThat(response.getStatus()).isEqualTo(Status.OK);
    }

    @Test
    public void testConstructor() {
        Response response = new Response(Status.TEAPOT);
        assertThat(response.getStatus()).isEqualTo(Status.TEAPOT);
    }

    @Test
    public void testDefensiveCopyIsEquivalent() throws Exception {
        Response response = new Response()
                .setVersion("1.0")
                .setEntity("Hello");
        response.getHeaders().put("X-Test", "Hello World");

        Response copy = new Response(response);

        assertThat(copy.getVersion()).isEqualTo(response.getVersion());
        assertThat(copy.getEntity().getString()).isEqualTo(response.getEntity().getString());
        assertThat(copy.getHeaders().keySet()).containsAll(response.getHeaders().keySet());
    }

    @Test
    public void testDefensiveCopyIsDetached() throws Exception {
        Response response = new Response()
                .setVersion("1.0")
                .setEntity("Hello");
        response.getHeaders().put("X-Test", "Hello World");

        Response copy = new Response(response);

        // Mutate the copied object
        response.setVersion("1.1")
                .setEntity("Bonjour");
        response.getHeaders().put("X-Test", "Bonjour");

        // Check that defensive copy has not changed
        assertThat(copy.getVersion()).isEqualTo("1.0");
        assertThat(copy.getEntity().getString()).isEqualTo("Hello");
        assertThat(copy.getHeaders().keySet()).contains("X-Test", "Content-Length");
    }
}
