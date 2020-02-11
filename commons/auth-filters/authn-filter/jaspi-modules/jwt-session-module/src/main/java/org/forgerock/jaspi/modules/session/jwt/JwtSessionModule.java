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

package org.forgerock.jaspi.modules.session.jwt;

import static org.forgerock.caf.authentication.framework.JaspiAdapters.adapt;
import static org.forgerock.util.promise.Promises.newExceptionPromise;
import static org.forgerock.util.promise.Promises.newResultPromise;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.message.AuthStatus;
import javax.security.auth.message.MessageInfo;
import javax.security.auth.message.MessagePolicy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.forgerock.caf.authentication.api.AsyncServerAuthModule;
import org.forgerock.caf.authentication.api.AuthenticationException;
import org.forgerock.caf.authentication.api.MessageInfoContext;
import org.forgerock.caf.authentication.framework.AuthenticationFramework;
import org.forgerock.caf.authentication.framework.JaspiAdapters;
import org.forgerock.services.context.AttributesContext;
import org.forgerock.http.header.SetCookieHeader;
import org.forgerock.http.protocol.Cookie;
import org.forgerock.http.protocol.Headers;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.RequestCookies;
import org.forgerock.http.protocol.Response;
import org.forgerock.json.jose.builders.JwtBuilderFactory;
import org.forgerock.json.jose.jwt.Jwt;
import org.forgerock.util.promise.Promise;

/**
 * A JASPI CHF Session Module which creates a JWT when securing the response from a successful authentication
 * and sets it as a Cookie on the response. Then on subsequent requests checks for the presents of the JWT as a
 * Cookie on the request and validates the signature and decrypts it and checks the expiration time of the JWT.
 */
public class JwtSessionModule extends AbstractJwtSessionModule<CookieWrapper> implements AsyncServerAuthModule {

    /**
     * Constructs an instance of the JwtSessionModule.
     */
    public JwtSessionModule() {
        super();
    }

    /**
     * Constructs an instance of the JwtSessionModule.
     *
     * @param jwtBuilderFactory An instance of the jwtBuilderFactory.
     */
    public JwtSessionModule(JwtBuilderFactory jwtBuilderFactory) {
        super(jwtBuilderFactory);
    }

    @Override
    public String getModuleId() {
        return "JwtSession";
    }

    @Override
    public Promise<Void, AuthenticationException> initialize(MessagePolicy requestPolicy, MessagePolicy responsePolicy,
            CallbackHandler handler, Map<String, Object> options) {
        try {
            initialize(handler, options);
            return newResultPromise(null);
        } catch (AuthenticationException e) {
            return newExceptionPromise(e);
        }
    }

    @Override
    public Collection<Class<?>> getSupportedMessageTypes() {
        return Arrays.asList(new Class<?>[]{Request.class, Response.class});
    }

    @Override
    public Promise<AuthStatus, AuthenticationException> validateRequest(MessageInfoContext messageInfo,
            Subject clientSubject, Subject serviceSubject) {
        try {
            return newResultPromise(validateRequest(adapt(messageInfo), clientSubject));
        } catch (AuthenticationException e) {
            return newExceptionPromise(e);
        }
    }

    @Override
    CookieWrapper findJwtSessionCookie(MessageInfo messageInfo) {
        Request request = (Request) messageInfo.getRequestMessage();
        RequestCookies cookies = request.getCookies();
        if (cookies.containsKey(sessionCookieName)) {
            for (Cookie cookie : cookies.get(sessionCookieName)) {
                return new CookieWrapper(cookie);
            }
        }
        return null;
    }

    @Override
    void setClaimsOnRequest(MessageInfo messageInfo, Jwt jwt) {
        Map<String, Object> claimsSetContext = jwt.getClaimsSet()
                .getClaim(AuthenticationFramework.ATTRIBUTE_AUTH_CONTEXT, Map.class);
        if (claimsSetContext != null) {
            MessageInfoContext messageInfoContext =
                    (MessageInfoContext) messageInfo.getMap().get(JaspiAdapters.MESSAGE_INFO_CONTEXT_KEY);
            Map<String, Object> requestAttributes = messageInfoContext.asContext(AttributesContext.class)
                    .getAttributes();
            for (String key : claimsSetContext.keySet()) {
                requestAttributes.put(key, claimsSetContext.get(key));
            }
        }
    }

    @Override
    Collection<CookieWrapper> createCookies(String value, int maxAge, String path) {
        List<CookieWrapper> cookies = new ArrayList<>();
        for (String cookieDomain : cookieDomains) {
            cookies.add(new CookieWrapper(new Cookie()
                    .setName(sessionCookieName)
                    .setValue(value)
                    .setMaxAge(maxAge)
                    .setPath(path)
                    .setDomain(cookieDomain)
                    .setSecure(isSecure)
                    .setHttpOnly(isHttpOnly)));
        }
        return cookies;
    }

    @Override
    void addCookiesToResponse(Collection<CookieWrapper> cookies, MessageInfo messageInfo) {
        Response response = (Response) messageInfo.getResponseMessage();
        Headers headers = response.getHeaders();
        for (CookieWrapper cookieWrapper : cookies) {
            headers.put(new SetCookieHeader(Collections.singletonList(cookieWrapper.getCookie())));
        }
    }

    @Override
    public Promise<AuthStatus, AuthenticationException> secureResponse(MessageInfoContext messageInfo,
            Subject serviceSubject) {
        try {
            return newResultPromise(secureResponse(adapt(messageInfo)));
        } catch (AuthenticationException e) {
            return newExceptionPromise(e);
        }
    }

    @Override
    String getPrincipalFromRequest(MessageInfo messageInfo) {
        MessageInfoContext messageInfoContext =
                (MessageInfoContext) messageInfo.getMap().get(JaspiAdapters.MESSAGE_INFO_CONTEXT_KEY);
        return (String) messageInfoContext.asContext(AttributesContext.class).getAttributes()
                .get(AuthenticationFramework.ATTRIBUTE_AUTH_PRINCIPAL);
    }

    @Override
    boolean isLogoutRequest(MessageInfo messageInfo) {
        MessageInfoContext messageInfoContext =
                (MessageInfoContext) messageInfo.getMap().get(JaspiAdapters.MESSAGE_INFO_CONTEXT_KEY);
        Map<String, Object> attributes = messageInfoContext.asContext(AttributesContext.class).getAttributes();
        return attributes.containsKey(LOGOUT_SESSION_REQUEST_ATTRIBUTE_NAME)
                ? (Boolean) attributes.get(LOGOUT_SESSION_REQUEST_ATTRIBUTE_NAME)
                : false;
    }

    @Override
    public Promise<Void, AuthenticationException> cleanSubject(MessageInfoContext messageInfo, Subject clientSubject) {
        return newResultPromise(null);
    }
}
