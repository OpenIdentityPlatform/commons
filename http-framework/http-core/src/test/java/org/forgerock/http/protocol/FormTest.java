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
 * Copyright 2010–2011 ApexIdentity Inc.
 * Portions Copyright 2011-2015 ForgeRock AS.
 */

package org.forgerock.http.protocol;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URISyntaxException;

import org.testng.annotations.Test;

@SuppressWarnings("javadoc")
public class FormTest {

    @Test
    public void fromFormString() {
        Form f = new Form().fromFormString("x=%3D+%20&y=z");
        assertThat(f.get("x").get(0)).isEqualTo("=  ");
        assertThat(f.get("y").get(0)).isEqualTo("z");
    }

    @Test
    public void fromFormStringAndBack() {
        String s1 = "x=%3D&y=%3F";
        String s2 = new Form().fromFormString(s1).toFormString();
        assertThat(s1).isEqualTo(s2);
    }

    @Test
    public void fromQueryString() {
        Form f = new Form().fromQueryString("x=%3D+%20%2B&y=z");
        assertThat(f.get("x").get(0)).isEqualTo("=  +");
        assertThat(f.get("y").get(0)).isEqualTo("z");
    }

    @Test
    public void fromQueryStringAndBack() {
        String s1 = "x=*+%20%2B&y=?";
        String s2 = new Form().fromQueryString(s1).toQueryString();
        assertThat(s2).isEqualTo("x=*%20%20%2B&y=?");
    }

    @Test
    public void fromRequestQuery() throws URISyntaxException {
        Request request = new Request();
        request.setUri("http://www.example.com/?x=%3D&y=%3F&z=a");
        Form f = new Form().fromRequestQuery(request);
        assertThat(f.get("x").get(0)).isEqualTo("=");
        assertThat(f.get("y").get(0)).isEqualTo("?");
        assertThat(f.get("z").get(0)).isEqualTo("a");
    }

    @Test
    void toRequestQuery() throws URISyntaxException {
        Request request = new Request();
        request.setUri("http://www.example.com/?x=%3D&y=%3F&z=a");
        Form f = new Form();
        f.add("foo", "bar");
        f.toRequestQuery(request);
        assertThat(f.get("x")).isNull();
        assertThat(f.get("y")).isNull();
        assertThat(f.get("z")).isNull();
        assertThat(f.get("foo").get(0)).isEqualTo("bar");
        assertThat(request.getUri().toString()).isEqualTo("http://www.example.com/?foo=bar");
    }

    @Test
    void appendRequestQuery() throws URISyntaxException {
        Request request = new Request();
        request.setUri("http://www.example.com/?x=%3D&y=%3F&z=a");
        Form f = new Form();
        f.add("foo", "bar");
        f.appendRequestQuery(request);
        f.fromRequestQuery(request);
        assertThat(f.get("x").get(0)).isEqualTo("=");
        assertThat(f.get("y").get(0)).isEqualTo("?");
        assertThat(f.get("z").get(0)).isEqualTo("a");
        assertThat(f.get("foo").get(0)).isEqualTo("bar");
        // predictable iteration order should ensure added params appear at the end
        assertThat(request.getUri().toASCIIString()).isEqualTo("http://www.example.com/?x=%3D&y=%3F&z=a&foo=bar");
    }
}
