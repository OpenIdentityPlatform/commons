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
 * Copyright 2014-2016 ForgeRock AS.
 * Portions copyright 2024 3A Systems LLC.
 */

package org.forgerock.caf.http;

import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.text.DateFormat;
import java.text.FieldPosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.caf.http.Cookie.addCookie;
import static org.forgerock.caf.http.Cookie.newCookie;
import static org.mockito.Mockito.*;

public class CookieTest {

    @AfterMethod
    public void cleanUp() {
        Cookie.isServlet3xPresent = true;
    }

    private Cookie createCookie(String name, String value, String path, String domain, int version, String comment,
            int expiry, boolean secure, boolean httpOnly) {
        Cookie cookie = newCookie(name, value);

        cookie.setPath(path);
        cookie.setDomain(domain);
        cookie.setVersion(version);
        cookie.setComment(comment);
        cookie.setMaxAge(expiry);
        cookie.setSecure(secure);
        cookie.setHttpOnly(httpOnly);

        return cookie;
    }

    @Test
    public void shouldAddServlet2xCookieToResponse() {

        //Given
        Cookie.isServlet3xPresent = false;
        Cookie cookie = createCookie("COOKIE_NAME", "COOKIE_VALUE", "/",
                "www.example.com", 0, "COOKIE_COMMENT", 6000,
                true, true);
        HttpServletResponse response = mock(HttpServletResponse.class);

        //When
        addCookie(cookie, response);

        DateFormat df = new SimpleDateFormat("EEE, dd-MMM-yyyy HH:mm:ss z", Locale.US);
        df.setTimeZone(TimeZone.getTimeZone("GMT"));

        //Then
        verify(response, never()).addCookie(Matchers.<jakarta.servlet.http.Cookie>anyObject());
        ArgumentCaptor<String> cookieCaptor = ArgumentCaptor.forClass(String.class);
        verify(response).addHeader(eq("Set-Cookie"), cookieCaptor.capture());
        assertThat(cookieCaptor.getValue()).contains("COOKIE_NAME=COOKIE_VALUE;", "Domain=www.example.com;",
                "Expires="+df.format(
                        new Date(System.currentTimeMillis() + 6000 * 1000L)));
    }

    @Test
    public void shouldAddServlet3xCookieToResponse() {

        //Given
        Cookie.isServlet3xPresent = true;
        Cookie cookie = createCookie("COOKIE_NAME", "COOKIE_VALUE", "/", "www.example.com", 0, "COOKIE_COMMENT", 6000,
                true, true);
        HttpServletResponse response = mock(HttpServletResponse.class);

        //When
        addCookie(cookie, response);

        //Then
        verify(response, never()).addHeader(eq("Set-Cookie"), anyString());
        ArgumentCaptor<jakarta.servlet.http.Cookie> cookieCaptor =
                ArgumentCaptor.forClass(jakarta.servlet.http.Cookie.class);
        verify(response).addCookie(cookieCaptor.capture());
        jakarta.servlet.http.Cookie servletCookie = cookieCaptor.getValue();
        assertThat(servletCookie.getName()).isEqualTo("COOKIE_NAME");
        assertThat(servletCookie.getValue()).isEqualTo("COOKIE_VALUE");
        assertThat(servletCookie.getPath()).isEqualTo("/");
        assertThat(servletCookie.getDomain()).isEqualTo("www.example.com");
//        assertThat(servletCookie.getVersion()).isEqualTo(0); //these are depreceated since 6.0 Servlet
//        assertThat(servletCookie.getComment()).isEqualTo("COOKIE_COMMENT");
        assertThat(servletCookie.getMaxAge()).isEqualTo(6000);
        assertThat(servletCookie.getSecure()).isEqualTo(true);
        assertThat(servletCookie.isHttpOnly()).isEqualTo(true);
    }

    @Test
    public void servletHasCookies() {

        //Given
        HttpServletRequest request = mock(HttpServletRequest.class);
        jakarta.servlet.http.Cookie[] cookies = new jakarta.servlet.http.Cookie[] {
            new jakarta.servlet.http.Cookie("mycookie", "somevalue")
        };

        //When
        when(request.getCookies()).thenReturn(cookies);

        //Then
        assertThat(Cookie.getCookies(request)).isNotNull();
        assertThat(Cookie.getCookies(request)).hasSize(1);
        Cookie cookie = Cookie.getCookies(request).iterator().next();
        assertThat(cookie.getName()).isEqualTo("mycookie");
        assertThat(cookie.getValue()).isEqualTo("somevalue");
    }

    @Test
    public void servletHasNoCookies() {

        //Given
        HttpServletRequest request = mock(HttpServletRequest.class);

        //When
        when(request.getCookies()).thenReturn(null);

        //Then
        assertThat(Cookie.getCookies(request)).isNotNull();
        assertThat(Cookie.getCookies(request)).hasSize(0);
    }
}
