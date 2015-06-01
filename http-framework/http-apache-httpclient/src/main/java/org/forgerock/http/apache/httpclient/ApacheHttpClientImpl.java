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
 * Copyright 2009 Sun Microsystems Inc.
 * Portions Copyright 2010–2011 ApexIdentity Inc.
 * Portions Copyright 2011-2015 ForgeRock AS.
 */

package org.forgerock.http.apache.httpclient;

import java.io.IOException;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.forgerock.http.client.ahc.AbstractClient;
import org.forgerock.http.io.Buffer;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.http.protocol.Status;
import org.forgerock.util.Factory;
import org.forgerock.util.promise.NeverThrowsException;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.promise.Promises;

/**
 * Apache HTTP Client implementation.
 */
final class ApacheHttpClientImpl extends AbstractClient {

    /** The Apache HTTP client to transmit requests through. */
    private final CloseableHttpClient httpClient;

    ApacheHttpClientImpl(final CloseableHttpClient httpClient, final Factory<Buffer> storage) {
        super(storage);
        this.httpClient = httpClient;
    }

    @Override
    public void close() throws IOException {
        httpClient.close();
    }

    @Override
    public Promise<Response, NeverThrowsException> sendAsync(final Request request) {
        try {
            // Convert the request to AHC then send it
            HttpUriRequest clientRequest = createHttpUriRequest(request);
            HttpResponse clientResponse = httpClient.execute(clientRequest);
            // Convert the AHC response back into OpenIG
            Response response = createResponse(clientResponse);
            return Promises.newResultPromise(response);
        } catch (final IOException e) {
            return Promises.newResultPromise(new Response().setStatus(Status.INTERNAL_SERVER_ERROR));
        }
    }

}
