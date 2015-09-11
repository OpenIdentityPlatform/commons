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

package org.forgerock.http.header;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Date;

import org.forgerock.http.protocol.Cookie;
import org.testng.annotations.Test;

/**
 * Unit tests for the Set-Cookie header class.
 * <p>
 * See <link>http://tools.ietf.org/html/rfc6265</link>
 * </p>
 */
public class SetCookieHeaderTest {

    private static final Date EXPIRES_DATE = new Date(1441963573000L);
    private static final String EXPIRES_DATE_STRING = "Fri, 11 Sep 2015 10:26:13 BST";

    @Test
    public void shouldCreateSetCookieHeaderWithNameAndValue() {

        //Given
        Cookie cookie = new Cookie()
                .setName("NAME")
                .setValue("VALUE");

        //When
        SetCookieHeader setCookieHeader = new SetCookieHeader(cookie);

        //Then
        assertThat(setCookieHeader.toString()).isEqualTo("NAME=VALUE");
    }

    @Test
    public void shouldCreateSetCookieHeaderWithExpires() {

        //Given
        Cookie cookie = new Cookie()
                .setName("NAME")
                .setValue("VALUE")
                .setExpires(EXPIRES_DATE);

        //When
        SetCookieHeader setCookieHeader = new SetCookieHeader(cookie);

        //Then
        assertThat(setCookieHeader.toString()).isEqualTo("NAME=VALUE; Expires=" + EXPIRES_DATE_STRING);
    }

    @Test
    public void shouldCreateSetCookieHeaderWithMaxAge() {

        //Given
        Cookie cookie = new Cookie()
                .setName("NAME")
                .setValue("VALUE")
                .setMaxAge(100);

        //When
        SetCookieHeader setCookieHeader = new SetCookieHeader(cookie);

        //Then
        assertThat(setCookieHeader.toString()).isEqualTo("NAME=VALUE; Max-Age=100");
    }

    @Test
    public void shouldCreateSetCookieHeaderWithPath() {

        //Given
        Cookie cookie = new Cookie()
                .setName("NAME")
                .setValue("VALUE")
                .setPath("/path");

        //When
        SetCookieHeader setCookieHeader = new SetCookieHeader(cookie);

        //Then
        assertThat(setCookieHeader.toString()).isEqualTo("NAME=VALUE; Path=/path");
    }

    @Test
    public void shouldCreateSetCookieHeaderWithDomain() {

        //Given
        Cookie cookie = new Cookie()
                .setName("NAME")
                .setValue("VALUE")
                .setDomain("DOMAIN");

        //When
        SetCookieHeader setCookieHeader = new SetCookieHeader(cookie);

        //Then
        assertThat(setCookieHeader.toString()).isEqualTo("NAME=VALUE; Domain=DOMAIN");
    }

    @Test
    public void shouldCreateSetCookieHeaderWithSecure() {

        //Given
        Cookie cookie = new Cookie()
                .setName("NAME")
                .setValue("VALUE")
                .setSecure(true);

        //When
        SetCookieHeader setCookieHeader = new SetCookieHeader(cookie);

        //Then
        assertThat(setCookieHeader.toString()).isEqualTo("NAME=VALUE; Secure");
    }

    @Test
    public void shouldCreateSetCookieHeaderWithHttpOnly() {

        //Given
        Cookie cookie = new Cookie()
                .setName("NAME")
                .setValue("VALUE")
                .setHttpOnly(true);

        //When
        SetCookieHeader setCookieHeader = new SetCookieHeader(cookie);

        //Then
        assertThat(setCookieHeader.toString()).isEqualTo("NAME=VALUE; HttpOnly");
    }

    @Test
    public void shouldCreateSetCookieHeaderWithAttributes() {

        //Given
        Cookie cookie = new Cookie()
                .setName("NAME")
                .setValue("VALUE")
                .setExpires(EXPIRES_DATE)
                .setMaxAge(100)
                .setPath("/path")
                .setDomain("DOMAIN")
                .setSecure(true)
                .setHttpOnly(true);

        //When
        SetCookieHeader setCookieHeader = new SetCookieHeader(cookie);

        //Then
        assertThat(setCookieHeader.toString()).isEqualTo("NAME=VALUE; Expires=" + EXPIRES_DATE_STRING
                + "; Max-Age=100; Path=/path; Domain=DOMAIN; Secure; HttpOnly");
    }

    @Test
    public void shouldCreateEmptySetCookieHeaderWhenCookieHasNoName() {

        //Given
        Cookie cookie = new Cookie()
                .setValue("VALUE")
                .setExpires(EXPIRES_DATE)
                .setMaxAge(100)
                .setPath("/path")
                .setDomain("DOMAIN")
                .setSecure(true)
                .setHttpOnly(true);

        //When
        SetCookieHeader setCookieHeader = new SetCookieHeader(cookie);

        //Then
        assertThat(setCookieHeader.toString()).isEmpty();
    }

    @Test
    public void shouldParseSetCookieHeaderWithNameAndValue() {

        //Given
        String cookieString = "NAME=VALUE";

        //When
        SetCookieHeader setCookieHeader = SetCookieHeader.valueOf(cookieString);

        //Then
        assertThat(setCookieHeader.getCookie().getName()).isEqualTo("NAME");
        assertThat(setCookieHeader.getCookie().getValue()).isEqualTo("VALUE");
        assertThat(setCookieHeader.getCookie().getExpires()).isNull();
        assertThat(setCookieHeader.getCookie().getMaxAge()).isNull();
        assertThat(setCookieHeader.getCookie().getPath()).isNull();
        assertThat(setCookieHeader.getCookie().getDomain()).isNull();
        assertThat(setCookieHeader.getCookie().isSecure()).isFalse();
        assertThat(setCookieHeader.getCookie().isHttpOnly()).isFalse();
    }

    @Test
    public void shouldParseSetCookieHeaderWithExpires() {

        //Given
        String cookieString = "NAME=VALUE; Expires=" + EXPIRES_DATE_STRING;

        //When
        SetCookieHeader setCookieHeader = SetCookieHeader.valueOf(cookieString);

        //Then
        assertThat(setCookieHeader.getCookie().getName()).isEqualTo("NAME");
        assertThat(setCookieHeader.getCookie().getValue()).isEqualTo("VALUE");
        assertThat(setCookieHeader.getCookie().getExpires()).isEqualTo(EXPIRES_DATE);
        assertThat(setCookieHeader.getCookie().getMaxAge()).isNull();
        assertThat(setCookieHeader.getCookie().getPath()).isNull();
        assertThat(setCookieHeader.getCookie().getDomain()).isNull();
        assertThat(setCookieHeader.getCookie().isSecure()).isFalse();
        assertThat(setCookieHeader.getCookie().isHttpOnly()).isFalse();
    }

    @Test
    public void shouldParseSetCookieHeaderWithMaxAge() {

        //Given
        String cookieString = "NAME=VALUE; Max-Age=100";

        //When
        SetCookieHeader setCookieHeader = SetCookieHeader.valueOf(cookieString);

        //Then
        assertThat(setCookieHeader.getCookie().getName()).isEqualTo("NAME");
        assertThat(setCookieHeader.getCookie().getValue()).isEqualTo("VALUE");
        assertThat(setCookieHeader.getCookie().getExpires()).isNull();
        assertThat(setCookieHeader.getCookie().getMaxAge()).isEqualTo(100);
        assertThat(setCookieHeader.getCookie().getPath()).isNull();
        assertThat(setCookieHeader.getCookie().getDomain()).isNull();
        assertThat(setCookieHeader.getCookie().isSecure()).isFalse();
        assertThat(setCookieHeader.getCookie().isHttpOnly()).isFalse();
    }

    @Test
    public void shouldParseSetCookieHeaderWithPath() {

        //Given
        String cookieString = "NAME=VALUE; Path=/path;";

        //When
        SetCookieHeader setCookieHeader = SetCookieHeader.valueOf(cookieString);

        //Then
        assertThat(setCookieHeader.getCookie().getName()).isEqualTo("NAME");
        assertThat(setCookieHeader.getCookie().getValue()).isEqualTo("VALUE");
        assertThat(setCookieHeader.getCookie().getExpires()).isNull();
        assertThat(setCookieHeader.getCookie().getMaxAge()).isNull();
        assertThat(setCookieHeader.getCookie().getPath()).isEqualTo("/path");
        assertThat(setCookieHeader.getCookie().getDomain()).isNull();
        assertThat(setCookieHeader.getCookie().isSecure()).isFalse();
        assertThat(setCookieHeader.getCookie().isHttpOnly()).isFalse();
    }

    @Test
    public void shouldParseSetCookieHeaderWithDomain() {

        //Given
        String cookieString = "NAME=VALUE; Domain=DOMAIN";

        //When
        SetCookieHeader setCookieHeader = SetCookieHeader.valueOf(cookieString);

        //Then
        assertThat(setCookieHeader.getCookie().getName()).isEqualTo("NAME");
        assertThat(setCookieHeader.getCookie().getValue()).isEqualTo("VALUE");
        assertThat(setCookieHeader.getCookie().getExpires()).isNull();
        assertThat(setCookieHeader.getCookie().getMaxAge()).isNull();
        assertThat(setCookieHeader.getCookie().getPath()).isNull();
        assertThat(setCookieHeader.getCookie().getDomain()).isEqualTo("DOMAIN");
        assertThat(setCookieHeader.getCookie().isSecure()).isFalse();
        assertThat(setCookieHeader.getCookie().isHttpOnly()).isFalse();
    }

    @Test
    public void shouldParseSetCookieHeaderWithSecure() {

        //Given
        String cookieString = "NAME=VALUE; Secure";

        //When
        SetCookieHeader setCookieHeader = SetCookieHeader.valueOf(cookieString);

        //Then
        assertThat(setCookieHeader.getCookie().getName()).isEqualTo("NAME");
        assertThat(setCookieHeader.getCookie().getValue()).isEqualTo("VALUE");
        assertThat(setCookieHeader.getCookie().getExpires()).isNull();
        assertThat(setCookieHeader.getCookie().getMaxAge()).isNull();
        assertThat(setCookieHeader.getCookie().getPath()).isNull();
        assertThat(setCookieHeader.getCookie().getDomain()).isNull();
        assertThat(setCookieHeader.getCookie().isSecure()).isTrue();
        assertThat(setCookieHeader.getCookie().isHttpOnly()).isFalse();
    }

    @Test
    public void shouldParseSetCookieHeaderWithHttpOnly() {

        //Given
        String cookieString = "NAME=VALUE; HttpOnly";

        //When
        SetCookieHeader setCookieHeader = SetCookieHeader.valueOf(cookieString);

        //Then
        assertThat(setCookieHeader.getCookie().getName()).isEqualTo("NAME");
        assertThat(setCookieHeader.getCookie().getValue()).isEqualTo("VALUE");
        assertThat(setCookieHeader.getCookie().getExpires()).isNull();
        assertThat(setCookieHeader.getCookie().getMaxAge()).isNull();
        assertThat(setCookieHeader.getCookie().getPath()).isNull();
        assertThat(setCookieHeader.getCookie().getDomain()).isNull();
        assertThat(setCookieHeader.getCookie().isSecure()).isFalse();
        assertThat(setCookieHeader.getCookie().isHttpOnly()).isTrue();
    }

    @Test
    public void shouldParseSetCookieHeaderWithAttributes() {

        //Given
        String cookieString = "NAME=VALUE; Expires=" + EXPIRES_DATE_STRING
                + "; Max-Age=100; Path=/path; Domain=DOMAIN; Secure; HttpOnly";

        //When
        SetCookieHeader setCookieHeader = SetCookieHeader.valueOf(cookieString);

        //Then
        assertThat(setCookieHeader.getCookie().getName()).isEqualTo("NAME");
        assertThat(setCookieHeader.getCookie().getValue()).isEqualTo("VALUE");
        assertThat(setCookieHeader.getCookie().getExpires()).isEqualTo(EXPIRES_DATE);
        assertThat(setCookieHeader.getCookie().getMaxAge()).isEqualTo(100);
        assertThat(setCookieHeader.getCookie().getPath()).isEqualTo("/path");
        assertThat(setCookieHeader.getCookie().getDomain()).isEqualTo("DOMAIN");
        assertThat(setCookieHeader.getCookie().isSecure()).isTrue();
        assertThat(setCookieHeader.getCookie().isHttpOnly()).isTrue();
    }

    @Test
    public void shouldParseEmptySetCookieHeaderWithCookieWithNoName() {

        //Given
        String cookieString = "=VALUE; Expires=" + EXPIRES_DATE_STRING
                + "; Max-Age=100; Path=/path; Domain=DOMAIN; Secure; HttpOnly";

        //When
        SetCookieHeader setCookieHeader = SetCookieHeader.valueOf(cookieString);

        //Then
        assertThat(setCookieHeader.getCookie().getName()).isNullOrEmpty();
        assertThat(setCookieHeader.getCookie().getValue()).isNull();
        assertThat(setCookieHeader.getCookie().getExpires()).isNull();
        assertThat(setCookieHeader.getCookie().getMaxAge()).isNull();
        assertThat(setCookieHeader.getCookie().getPath()).isNull();
        assertThat(setCookieHeader.getCookie().getDomain()).isNull();
        assertThat(setCookieHeader.getCookie().isSecure()).isFalse();
        assertThat(setCookieHeader.getCookie().isHttpOnly()).isFalse();
    }
}
