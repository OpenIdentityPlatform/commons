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
*/

package org.forgerock.jaspi.modules.openid.helpers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

/**
 * Simple helper client for connecting to URLs over HTTP
 * and retrieving their contents via a GET request.
 *
 * Settable timeouts on read and connection.
 */
public class SimpleHTTPClient {

    /**
     * Default read timeout on HTTP requests from this client.
     */
    private static final int DEFAULT_READ_TIMEOUT = 5_000;

    /**
     * Default connection timeout on HTTP requests from this client.
     */
    private static final int DEFAULT_CONNECTION_TIMEOUT = 5_000;

    private final int readTimeout;
    private final int connTimeout;

    /**
     * Generates a new SimpleHTTPClient with the appropriate timeouts.
     *
     * Invalid timeout values (less than zero) will use the default timeout value of this class.
     *
     * @param readTimeout read timeout value (greater than or equal to zero)
     * @param connTimeout connection timeout value (greater than or equal to zero)
     */
    public SimpleHTTPClient(final int readTimeout, final int connTimeout) {

        if (readTimeout < 0 || connTimeout < 0) {
            throw new IllegalArgumentException("Unable to set the read or connection timeouts "
                    + "to a value less than zero");
        }

        this.readTimeout = readTimeout;
        this.connTimeout = connTimeout;
    }

    /**
     * Utility method for gathering the contents of an HTTP page.
     *
     * Should ideally be in an HTTP Client utils type package, rather than here.
     *
     * @param url from which to attempt to retrieve the contents
     * @return The contents of the provided url
     * @throws java.io.IOException If there are any problems connecting to or gathering the contents of the page
     */
    public String get(final URL url) throws IOException {
        final URLConnection conn = url.openConnection();

        if (readTimeout >= 0) {
            conn.setReadTimeout(readTimeout);
        } else {
            conn.setReadTimeout(DEFAULT_READ_TIMEOUT);
        }

        if (connTimeout >= 0) {
            conn.setConnectTimeout(connTimeout);
        } else {
            conn.setConnectTimeout(DEFAULT_CONNECTION_TIMEOUT);
        }

        final StringBuilder sb = new StringBuilder();

        try (final BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            String input;
            while ((input = reader.readLine()) != null) {
                sb.append(input);
            }
        }

        return sb.toString();
    }

}
