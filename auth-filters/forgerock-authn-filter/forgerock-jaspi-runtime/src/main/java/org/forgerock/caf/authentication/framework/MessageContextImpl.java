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

package org.forgerock.caf.authentication.framework;

import java.util.HashMap;
import java.util.Map;

import org.forgerock.caf.authentication.api.AsyncServerAuthContext;
import org.forgerock.caf.authentication.api.AuthContextWithState;
import org.forgerock.caf.authentication.api.AuthenticationState;
import org.forgerock.caf.authentication.api.MessageContext;
import org.forgerock.caf.authentication.api.MessageInfoContext;
import org.forgerock.http.Context;
import org.forgerock.http.context.ServerContext;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.http.protocol.Status;
import org.forgerock.util.Reject;

/**
 * An implementation of {@link MessageContext} that holds contextual information and state for a
 * given request and response message exchange.
 *
 * @see MessageContext
 * @see MessageInfoContext
 *
 * @since 2.0.0
 */
final class MessageContextImpl extends ServerContext implements MessageContext {

    private Request request;
    private Response response;
    private AuditTrail auditTrail;
    private Map<String, Object> requestContextMap = new HashMap<>();
    private final Map<Class<? extends AsyncServerAuthContext>, AuthenticationState> authContextState = new HashMap<>();

    /**
     * Creates a new message context that holds both the request and response messages and the
     * global context {@code Map}.
     *
     * @param parent The parent context.
     * @param request The request message.
     * @param auditTrail The {@code AuditTrail} instance.
     */
    MessageContextImpl(Context parent, Request request, AuditTrail auditTrail) {
        super(parent, "jaspi");
        Reject.ifNull(request, auditTrail);
        this.request = request;
        this.response = new Response().setStatus(Status.OK);
        this.auditTrail = auditTrail;
    }

    @Override
    public Request getRequest() {
        return request;
    }

    @Override
    public void setRequest(Request request) {
        this.request = request;
    }

    @Override
    public Response getResponse() {
        return response;
    }

    @Override
    public void setResponse(Response response) {
        this.response = response;
    }

    @Override
    public Map<String, Object> getRequestContextMap() {
        return requestContextMap;
    }

    @Override
    public AuditTrail getAuditTrail() {
        return auditTrail;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends AuthenticationState> T getState(AsyncServerAuthContext authContext) {
        AuthenticationState state = authContextState.get(authContext.getClass());
        if (state == null) {
            if (authContext instanceof AuthContextWithState) {
                state = ((AuthContextWithState) authContext).createAuthenticationState();
            } else {
                state = new AuthenticationState();
            }
            authContextState.put(authContext.getClass(), state);
        }
        return (T) state;
    }
}
