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

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

import org.forgerock.http.header.CookieHeader;
import org.testng.annotations.Test;

@SuppressWarnings("javadoc")
public class RequestTest {

    @Test
    public void testMethodChaining() {
        Request request = new Request().setVersion("123").setMethod("GET");
        assertThat(request.getVersion()).isEqualTo("123");
        assertThat(request.getMethod()).isEqualTo("GET");
    }

    @Test
    public void testDefensiveCopyIsEquivalent() throws Exception {
        Request request = new Request()
                .setUri("http://openig.example.com?query=abc")
                .setMethod("POST")
                .setVersion("1.0")
                .setEntity("Hello");
        request.getHeaders().put("X-Test", "Hello World");

        Cookie cookie = new Cookie().setName("a").setValue("b");
        request.getHeaders().put(new CookieHeader(singletonList(cookie)));

        Request copy = new Request(request);

        assertThat(copy.getUri()).isEqualTo(request.getUri());
        assertThat(copy.getMethod()).isEqualTo(request.getMethod());
        assertThat(copy.getVersion()).isEqualTo(request.getVersion());
        assertThat(copy.getEntity().getString()).isEqualTo(request.getEntity().getString());
        assertThat(copy.getHeaders().keySet()).containsAll(request.getHeaders().keySet());

        assertThat(copy.getForm().toQueryString()).isEqualTo(request.getForm().toQueryString());

        assertThat(copy.getCookies()).hasSize(1);
        assertThat(copy.getCookies().get("a")).hasSize(1);
        assertThat(copy.getCookies().get("a").get(0)).isEqualTo(cookie);
    }

    @Test
    public void testDefensiveCopyIsDetached() throws Exception {
        Request request = new Request()
                .setUri("http://openig.example.com?query=abc")
                .setMethod("POST")
                .setVersion("1.0")
                .setEntity("Hello");
        request.getHeaders().put("X-Test", "Hello World");

        Cookie cookie = new Cookie().setName("a").setValue("b");
        request.getHeaders().put(new CookieHeader(singletonList(cookie)));

        Request copy = new Request(request);

        // Mutate the copied object
        request.setUri("http://openam.forgerock.org")
                .setVersion("1.1")
                .setMethod("DELETE")
                .setEntity("Bonjour");
        request.getHeaders().put("X-Test", "Bonjour");
        request.getHeaders().remove("Cookie");

        // Check that defensive copy has not changed
        assertThat(copy.getUri().toASCIIString()).isEqualTo("http://openig.example.com?query=abc");
        assertThat(copy.getMethod()).isEqualTo("POST");
        assertThat(copy.getVersion()).isEqualTo("1.0");
        assertThat(copy.getEntity().getString()).isEqualTo("Hello");
        assertThat(copy.getHeaders().keySet()).contains("X-Test", "Cookie", "Content-Length");

        assertThat(copy.getForm().toQueryString()).isEqualTo("query=abc");

        assertThat(copy.getCookies()).hasSize(1);
        assertThat(copy.getCookies().get("a")).hasSize(1);
        assertThat(copy.getCookies().get("a").get(0)).isEqualTo(cookie);
    }
}
