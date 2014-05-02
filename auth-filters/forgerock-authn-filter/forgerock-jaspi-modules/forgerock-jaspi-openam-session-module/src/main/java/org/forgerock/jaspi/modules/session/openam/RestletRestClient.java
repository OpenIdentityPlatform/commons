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
 * Copyright 2014 ForgeRock AS.
 */

package org.forgerock.jaspi.modules.session.openam;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.forgerock.auth.common.DebugLogger;
import org.forgerock.jaspi.logging.LogFactory;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ResourceException;
import org.json.JSONObject;
import org.restlet.Context;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.ext.ssl.DefaultSslContextFactory;
import org.restlet.ext.ssl.SslContextFactory;
import org.restlet.resource.ClientResource;

import java.util.Map;

import static org.forgerock.json.fluent.JsonValue.json;
import static org.forgerock.json.fluent.JsonValue.object;

/**
 * Simple REST client implemented using Restlet.
 *
 * @since 1.4.0
 */
class RestletRestClient implements RestClient {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final DebugLogger logger = LogFactory.getDebug();

    private volatile SslContextFactory sslContextFactory;

    /**
     * {@inheritDoc}
     */
    @Override
    public void setSslConfiguration(final SslConfiguration sslConfiguration) {

        final DefaultSslContextFactory sslContextFactory = new DefaultSslContextFactory();
        sslContextFactory.setKeyManagerAlgorithm(sslConfiguration.getKeyManagerAlgorithm());
        sslContextFactory.setKeyStorePath(sslConfiguration.getKeyStorePath());
        sslContextFactory.setKeyStoreType(sslConfiguration.getKeyStoreType());
        sslContextFactory.setKeyStorePassword(sslConfiguration.getKeyStorePassword());
        sslContextFactory.setKeyStoreKeyPassword(sslConfiguration.getKeyStoreKeyPassword());

        sslContextFactory.setTrustManagerAlgorithm(sslConfiguration.getTrustManagerAlgorithm());
        sslContextFactory.setTrustStorePath(sslConfiguration.getTrustStorePath());
        sslContextFactory.setTrustStoreType(sslConfiguration.getTrustStoreType());
        sslContextFactory.setTrustStorePassword(sslConfiguration.getTrustStorePassword());

        this.sslContextFactory = sslContextFactory;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JsonValue post(final String uri, final Map<String, String> queryParameters) throws ResourceException {

        final ClientResource resource = createResource(uri);
        for (final Map.Entry<String, String> entry : queryParameters.entrySet()) {
            resource.addQueryParameter(entry.getKey(), entry.getValue());
        }

        if (sslContextFactory != null) {
            logger.debug("Making REST call to validate SSO Token using SSL");
            resource.getContext().getAttributes().put("sslContextFactory", sslContextFactory);
        }

        try {
            final JSONObject response = resource.post(new JsonRepresentation(json(object()).toString()),
                    JSONObject.class);

            return new JsonValue(OBJECT_MAPPER.readValue(response.toString(), Map.class));
        } catch (org.restlet.resource.ResourceException e) {
            throw ResourceException.getException(e.getStatus().getCode(), e.getMessage());
        } catch (Exception e) {
            throw ResourceException.getException(ResourceException.INTERNAL_ERROR, e.getMessage());
        }
    }

    /**
     * Creates the Restlet ClientResource.
     *
     * @param uri The resource URI.
     * @return The ClientResource instance.
     */
    ClientResource createResource(final String uri) {
        return new ClientResource(new Context(), uri);
    }
}
