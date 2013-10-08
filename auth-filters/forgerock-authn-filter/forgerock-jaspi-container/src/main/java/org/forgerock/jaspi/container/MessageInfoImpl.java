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
 * Copyright 2013 ForgeRock Inc.
 */

package org.forgerock.jaspi.container;

import javax.security.auth.message.MessageInfo;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.util.HashMap;
import java.util.Map;

/**
 * Used to pass messages and message processing state to authentication contexts for processing by authentication
 * modules.
 *
 * @author Phill Cunningon
 * @since 1.0.0
 */
public class MessageInfoImpl implements MessageInfo {

    private Map<String, Object> map;
    private HttpServletRequestWrapper requestWrapper;
    private HttpServletResponseWrapper responseWrapper;

    /**
     * Required no argument constructor.
     */
    public MessageInfoImpl() {
    }

    /**
     * Required single argument constructor.
     *
     * @param map A map of key pairs.
     */
    public MessageInfoImpl(Map<String, Object> map) {
        this.map = map;
    }

    /**
     * Get the request message object from this MessageInfo.
     *
     * @return An object representing the request message, or null if no request message is set within the MessageInfo.
     */
    public HttpServletRequestWrapper getRequestMessage() {
        return requestWrapper;
    }

    /**
     * Get the response message object from this MessageInfo.
     *
     * @return An object representing the response message, or null if no response message is set within the
     * MessageInfo.
     */
    public HttpServletResponseWrapper getResponseMessage() {
        return responseWrapper;
    }

    /**
     * Set the request message HttpServletRequest in this MessageInfo.
     *
     * @param request An HttpServletRequest representing the request message.
     */
    public void setRequestMessage(Object request) {
        HttpServletRequestWrapper wrapper = new HttpServletRequestWrapper((HttpServletRequest) request);
        this.requestWrapper = wrapper;
    }

    /**
     * Set the response message HttpServletResponse in this MessageInfo.
     *
     * @param response An HttpServletResponse representing the response message.
     */
    public void setResponseMessage(Object response) {
        HttpServletResponseWrapper wrapper = new HttpServletResponseWrapper((HttpServletResponse) response);
        this.responseWrapper = wrapper;
    }

    /**
     * Get (a reference to) the Map object of this MessageInfo. Operations performed on the acquired Map must effect
     * the Map within the MessageInfo.
     *
     * @return The Map object of this MessageInfo. This method never returns null. If a Map has not been associated
     * with the MessageInfo, this method instantiates a Map, associates it with this MessageInfo, and then returns it.
     */
    public Map<String, Object> getMap() {
        if (map == null) {
            map = new HashMap<String, Object>();
        }
        return map;
    }
}
