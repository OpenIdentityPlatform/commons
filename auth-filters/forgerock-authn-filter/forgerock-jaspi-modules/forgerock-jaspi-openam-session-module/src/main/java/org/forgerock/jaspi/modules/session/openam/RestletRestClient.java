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
 * Copyright 2014-2015 ForgeRock AS.
 */

package org.forgerock.jaspi.modules.session.openam;

import static org.forgerock.caf.authentication.framework.AuthenticationFramework.LOG;
import static org.forgerock.json.fluent.JsonValue.json;
import static org.forgerock.json.fluent.JsonValue.object;

import java.io.IOException;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ResourceException;
import org.json.JSONObject;
import org.restlet.Context;
import org.restlet.engine.header.Header;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.ext.ssl.DefaultSslContextFactory;
import org.restlet.ext.ssl.SslContextFactory;
import org.restlet.resource.ClientResource;
import org.restlet.util.Series;

/**
 * Simple REST client implemented using Restlet.
 *
 * @since 1.4.0
 */
class RestletRestClient implements RestClient {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

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
    public JsonValue get(String uri, Map<String, String> queryParameters, Map<String, String> headers)
            throws ResourceException {

        final ClientResource resource = createClientResource(uri, queryParameters, headers);

        try {
            final JSONObject response = resource.get(JSONObject.class);
            return convertResponse(response);
        } catch (org.restlet.resource.ResourceException e) {
            LOG.error("REST GET request failed.", e);
            throw ResourceException.getException(e.getStatus().getCode(), e.getMessage());
        } catch (Exception e) {
            LOG.error("REST GET request failed.", e);
            throw ResourceException.getException(ResourceException.INTERNAL_ERROR, e.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JsonValue post(String uri, Map<String, String> queryParameters, Map<String, String> headers)
            throws ResourceException {

        final ClientResource resource = createClientResource(uri, queryParameters, headers);

        try {
            final JSONObject response = resource.post(new JsonRepresentation(json(object()).toString()),
                    JSONObject.class);

            return convertResponse(response);
        } catch (org.restlet.resource.ResourceException e) {
            LOG.error("REST POST request failed.", e);
            throw ResourceException.getException(e.getStatus().getCode(), e.getMessage());
        } catch (Exception e) {
            LOG.error("REST POST request failed.", e);
            throw ResourceException.getException(ResourceException.INTERNAL_ERROR, e.getMessage());
        }
    }

    /**
     * Create the Restlet ClientResource and sets the query parameters and headers.
     *
     * @param uri The resource URI.
     * @param queryParameters The query parameters to set on the REST request.
     * @param headers The headers to set on the REST request.
     * @return The ClientResource.
     */
    @SuppressWarnings("unchecked")
    private ClientResource createClientResource(String uri, Map<String, String> queryParameters,
            Map<String, String> headers) {

        final ClientResource resource = createResource(uri);
        for (final Map.Entry<String, String> entry : queryParameters.entrySet()) {
            resource.addQueryParameter(entry.getKey(), entry.getValue());
        }

        resource.getRequest().getAttributes().putIfAbsent("org.restlet.http.headers", new Series<Header>(Header.class));
        final Series<Header> resourceHeaders = (Series<Header>) resource.getRequestAttributes()
                .get("org.restlet.http.headers");
        for (final Map.Entry<String, String> entry : headers.entrySet()) {
            resourceHeaders.set(entry.getKey(), entry.getValue());
        }

        if (sslContextFactory != null) {
            LOG.debug("Making REST call to validate SSO Token using SSL");
            resource.getContext().getAttributes().put("sslContextFactory", sslContextFactory);
        }

        return resource;
    }

    /**
     * Converts the JSONObject response from Restlet to a JsonValue.
     *
     * @param response The REST response.
     * @return The JsonValue representation of the response.
     * @throws IOException If there is a problem parsing the response.
     */
    private JsonValue convertResponse(JSONObject response) throws IOException {
        return new JsonValue(OBJECT_MAPPER.readValue(response.toString(), Map.class));
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
