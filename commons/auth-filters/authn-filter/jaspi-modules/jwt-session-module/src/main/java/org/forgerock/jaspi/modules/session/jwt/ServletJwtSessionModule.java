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
 * Copyright 2015-2016 ForgeRock AS.
 */

package org.forgerock.jaspi.modules.session.jwt;

import static org.forgerock.caf.http.Cookie.getCookies;
import static org.forgerock.caf.http.Cookie.newCookie;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.message.AuthException;
import javax.security.auth.message.AuthStatus;
import javax.security.auth.message.MessageInfo;
import javax.security.auth.message.MessagePolicy;
import javax.security.auth.message.module.ServerAuthModule;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.forgerock.caf.authentication.framework.AuthenticationFramework;
import org.forgerock.caf.http.Cookie;
import org.forgerock.json.jose.builders.JwtBuilderFactory;
import org.forgerock.json.jose.jwt.Jwt;

/**
 * A JASPI Servlet API Session Module which creates a JWT when securing the response from a successful authentication
 * and sets it as a Cookie on the response. Then on subsequent requests checks for the presents of the JWT as a
 * Cookie on the request and validates the signature and decrypts it and checks the expiration time of the JWT.
 */
public class ServletJwtSessionModule extends AbstractJwtSessionModule<Cookie> implements ServerAuthModule {

    /**
     * Constructs an instance of the ServletJwtSessionModule.
     */
    public ServletJwtSessionModule() {
        super();
    }

    /**
     * Constructs an instance of the ServletJwtSessionModule.
     *
     * @param jwtBuilderFactory An instance of the jwtBuilderFactory.
     */
    public ServletJwtSessionModule(JwtBuilderFactory jwtBuilderFactory) {
        super(jwtBuilderFactory);
    }

    @Override
    public void initialize(MessagePolicy requestPolicy, MessagePolicy responsePolicy, CallbackHandler callbackHandler,
            Map options) throws AuthException {
        initialize(callbackHandler, options);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class[] getSupportedMessageTypes() {
        return new Class[]{HttpServletRequest.class, HttpServletResponse.class};
    }

    @Override
    public AuthStatus validateRequest(MessageInfo messageInfo, Subject clientSubject, Subject serviceSubject)
            throws AuthException {
        return validateRequest(messageInfo, clientSubject);
    }

    @Override
    public Jwt validateJwtSessionCookie(MessageInfo messageInfo) {
        return super.validateJwtSessionCookie(messageInfo);
    }

    /**
     * Find a session cookie in the given message info.
     * @param messageInfo The message info.
     * @return The cookie, or null.
     */
    public Cookie findJwtSessionCookie(MessageInfo messageInfo) {
        HttpServletRequest request = (HttpServletRequest) messageInfo.getRequestMessage();
        Set<Cookie> cookies = getCookies(request);
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (sessionCookieName.equals(cookie.getName())) {
                    return cookie;
                }
            }
        }
        return null;
    }

    @Override
    void setClaimsOnRequest(MessageInfo messageInfo, Jwt jwt) {
        HttpServletRequest request = (HttpServletRequest) messageInfo.getRequestMessage();
        Map<String, Object> claimsSetContext = jwt.getClaimsSet()
                .getClaim(AuthenticationFramework.ATTRIBUTE_AUTH_CONTEXT, Map.class);
        if (claimsSetContext != null) {
            for (String key : claimsSetContext.keySet()) {
                request.setAttribute(key, claimsSetContext.get(key));
            }
        }
    }

    @Override
    Collection<Cookie> createCookies(String value, int maxAge, String path) {
        Collection<Cookie> cookies = new HashSet<>();
        for (String cookieDomain : cookieDomains) {
            Cookie cookie = newCookie(sessionCookieName, value);
            cookie.setMaxAge(maxAge);
            cookie.setPath(path);
            cookie.setDomain(cookieDomain);
            cookie.setSecure(isSecure);
            cookie.setHttpOnly(isHttpOnly);
            cookies.add(cookie);
        }
        return cookies;
    }

    @Override
    void addCookiesToResponse(Collection<Cookie> cookies, MessageInfo messageInfo) {
        HttpServletResponse response = (HttpServletResponse) messageInfo.getResponseMessage();
        Cookie.addCookies(cookies, response);
    }

    @Override
    public AuthStatus secureResponse(MessageInfo messageInfo, Subject subject) throws AuthException {
        return secureResponse(messageInfo);
    }

    @Override
    String getPrincipalFromRequest(MessageInfo messageInfo) {
        HttpServletRequest request = (HttpServletRequest) messageInfo.getRequestMessage();
        return (String) request.getAttribute(AuthenticationFramework.ATTRIBUTE_AUTH_PRINCIPAL);
    }

    @Override
    boolean isLogoutRequest(MessageInfo messageInfo) {
        HttpServletRequest request = (HttpServletRequest) messageInfo.getRequestMessage();
        Object isLogoutRequest = request.getAttribute(LOGOUT_SESSION_REQUEST_ATTRIBUTE_NAME);
        return isLogoutRequest != null && (Boolean) request.getAttribute(LOGOUT_SESSION_REQUEST_ATTRIBUTE_NAME);
    }

    @Override
    public void cleanSubject(MessageInfo messageInfo, Subject subject) {
    }
}
