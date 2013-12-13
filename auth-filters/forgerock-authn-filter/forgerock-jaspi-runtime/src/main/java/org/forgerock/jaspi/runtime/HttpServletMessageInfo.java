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
 * Copyright 2013 ForgeRock AS.
 */

package org.forgerock.jaspi.runtime;

import org.forgerock.jaspi.runtime.response.JaspiHttpServletResponseWrapper;

import javax.security.auth.message.MessageInfo;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.util.HashMap;
import java.util.Map;

/**
 * Provides an implementation of a MessageInfo for the HttpServlet profile.
 *
 * @since 1.3.0
 */
public class HttpServletMessageInfo implements MessageInfo {

    private HttpServletRequestWrapper request;
    private HttpServletResponseWrapper response;
    private final Map<String, Object> properties;

    /**
     * Constructs a new HttpServletMessageInfo.
     *
     * @param request The HttpServletRequest.
     * @param response The HtwtpServletResponse.
     */
    public HttpServletMessageInfo(final HttpServletRequest request, final HttpServletResponse response) {
        this(request, response, new HashMap<String, Object>());
    }

    /**
     * Constructs a new HttpServletMessageInfo.
     *
     * @param request The HttpServletRequest.
     * @param response The HttpServletResponse.
     * @param properties A Map of properties.
     */
    public HttpServletMessageInfo(final HttpServletRequest request, final HttpServletResponse response,
            final Map<String, Object> properties) {
        setRequestMessage(request);
        setResponseMessage(response);
        this.properties = properties;
    }

    /**
     * Get the request message object from this MessageInfo.
     *
     * @return An object representing the request message, or null if no request message is set within the MessageInfo.
     */
    @Override
    public HttpServletRequest getRequestMessage() {
        return request;
    }

    /**
     * Get the response message object from this MessageInfo.
     *
     * @return An object representing the response message, or null if no response message is set within the
     * MessageInfo.
     */
    @Override
    public HttpServletResponse getResponseMessage() {
        return response;
    }

    /**
     * Set the request message object in this MessageInfo.
     *
     * @param request An object representing the request message.
     */
    @Override
    public void setRequestMessage(final Object request) {
        this.request = new HttpServletRequestWrapper((HttpServletRequest) request);
    }

    /**
     * Set the response message object in this MessageInfo.
     *
     * @param response An object representing the response message.
     */
    @Override
    public void setResponseMessage(final Object response) {
        this.response = new JaspiHttpServletResponseWrapper((HttpServletResponse) response);
    }

    /**
     * Get (a reference to) the Map object of this MessageInfo.
     *
     * @return The Map object of this MessageInfo.
     */
    @Override
    public Map<String, Object> getMap() {
        return properties;
    }
}
