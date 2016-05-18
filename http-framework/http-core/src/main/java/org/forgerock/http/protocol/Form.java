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
 * Portions Copyright 2011-2016 ForgeRock AS.
 */

package org.forgerock.http.protocol;


import static org.forgerock.http.util.Uris.formDecodeParameterNameOrValue;
import static org.forgerock.http.util.Uris.formEncodeParameterNameOrValue;
import static org.forgerock.http.util.Uris.urlDecodeQueryParameterNameOrValue;
import static org.forgerock.http.util.Uris.urlEncodeQueryParameterNameOrValue;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.LinkedHashMap;
import java.util.List;

import org.forgerock.http.header.ContentLengthHeader;
import org.forgerock.http.header.ContentTypeHeader;
import org.forgerock.http.util.MultiValueMap;

/**
 * Form fields, a case-sensitive multi-string-valued map. The form can be read
 * from and written to request objects as query parameters (GET) and request
 * entities (POST).
 */
public class Form extends MultiValueMap<String, String> {

    /**
     * Constructs a new, empty form object.
     */
    public Form() {
        super(new LinkedHashMap<String, List<String>>());
    }

    /**
     * Parses a form URL-encoded string containing form parameters and stores them in
     * this object. Malformed name-value pairs (missing the "=" delimiter) are
     * simply ignored.
     *
     * @param s the form URL-encoded string to parse.
     * @return this form object.
     * @deprecated use {@link #fromFormString(String)} instead.
     */
    @Deprecated
    public Form fromString(String s) {
        return fromFormString(s);
    }

    /**
     * Parses a form URL-encoded string containing form parameters and stores them in
     * this object. Malformed name-value pairs (missing the "=" delimiter) are
     * simply ignored.
     *
     * @param s the form URL-encoded string to parse.
     * @return this form object.
     */
    public Form fromFormString(final String s) {
        for (String param : s.split("&")) {
            String[] nv = param.split("=", 2);
            if (nv.length == 2) {
                add(formDecodeParameterNameOrValue(nv[0]), formDecodeParameterNameOrValue(nv[1]));
            }
        }
        return this;
    }

    /**
     * Parses a URL-encoded query string containing form parameters and stores them in
     * this object. Malformed name-value pairs (missing the "=" delimiter) are
     * simply ignored.
     *
     * @param s the URL-encoded query string to parse.
     * @return this form object.
     */
    public Form fromQueryString(final String s) {
        for (String param : s.split("&")) {
            String[] nv = param.split("=", 2);
            if (!nv[0].isEmpty()) {
                add(urlDecodeQueryParameterNameOrValue(nv[0]),
                        nv.length == 1 ? null : urlDecodeQueryParameterNameOrValue(nv[1]));
            }
        }
        return this;
    }

    /**
     * Returns this form in a form URL-encoded string.
     *
     * @return the form URL-encoded form.
     * @deprecated use {@link #toFormString()} instead.
     */
    @Deprecated
    @Override
    public String toString() {
        return toFormString();
    }

    /**
     * Returns this form in a form URL-encoded string.
     *
     * @return the form URL-encoded form.
     */
    public String toFormString() {
        StringBuilder sb = new StringBuilder();
        for (String name : keySet()) {
            if (!name.isEmpty()) {
                for (String value : get(name)) {
                    if (value != null) {
                        if (sb.length() > 0) {
                            sb.append('&');
                        }
                        sb.append(formEncodeParameterNameOrValue(name))
                                .append('=')
                                .append(formEncodeParameterNameOrValue(value));
                    }
                }
            }
        }
        return sb.toString();
    }

    /**
     * Returns this form in a URL-encoded query string.
     *
     * @return the URL-encoded query string.
     */
    public String toQueryString() {
        StringBuilder sb = new StringBuilder();
        for (String name : keySet()) {
            if (!name.isEmpty()) {
                for (String value : get(name)) {
                    if (sb.length() > 0) {
                        sb.append('&');
                    }
                    sb.append(urlEncodeQueryParameterNameOrValue(name));
                    if (value != null) {
                        sb.append('=').append(urlEncodeQueryParameterNameOrValue(value));
                    }
                }
            }
        }
        return sb.toString();
    }

    /**
     * Parses the query parameters of a request and stores them in this object.
     * The object is not cleared beforehand, so this adds to any fields already
     * in place.
     *
     * @param request the request to be parsed.
     * @return this form object.
     */
    public Form fromRequestQuery(Request request) {
        String query = request.getUri().getRawQuery();
        if (query != null) {
            fromQueryString(query);
        }
        return this;
    }

    /**
     * Sets a request URI with query parameters. This overwrites any query
     * parameters that may already exist in the request URI.
     *
     * @param request the request to set query parameters to.
     */
    public void toRequestQuery(Request request) {
        try {
            request.getUri().setRawQuery(toQueryString());
        } catch (URISyntaxException use) {
            throw new IllegalArgumentException(use);
        }
    }

    /**
     * Appends the form as additional query parameters on an existing request
     * URI. This leaves any existing query parameters intact.
     *
     * @param request the request to append query parameters to.
     */
    public void appendRequestQuery(Request request) {
        StringBuilder sb = new StringBuilder();
        String uriQuery = request.getUri().getRawQuery();
        if (uriQuery != null && uriQuery.length() > 0) {
            sb.append(uriQuery);
        }
        String toAppend = toQueryString();
        if (toAppend != null && toAppend.length() > 0) {
            if (sb.length() > 0) {
                sb.append('&');
            }
            sb.append(toAppend);
        }
        String newQuery = sb.toString();
        if (newQuery.length() == 0) {
            newQuery = null;
        }
        try {
            request.getUri().setRawQuery(newQuery);
        } catch (URISyntaxException use) {
            throw new IllegalArgumentException(use);
        }
    }

    /**
     * Parses the URL-encoded form entity of a request and stores them in this
     * object. The object is not cleared beforehand, so this adds to any fields
     * already in place.
     *
     * @param request
     *            the request to be parsed.
     * @return this form object.
     * @throws IOException
     *             if an I/O exception occurs.
     */
    public Form fromRequestEntity(Request request) throws IOException {
        if (request != null
                && request.getEntity() != null
                && "application/x-www-form-urlencoded".equalsIgnoreCase(request.getHeaders()
                        .getFirst(ContentTypeHeader.class))) {
            fromFormString(request.getEntity().getString());
        }
        return this;
    }

    /**
     * Populates a request with the necessary headers and entity for the form to
     * be submitted as a POST with application/x-www-form-urlencoded content
     * type. This overwrites any entity that may already be in the request.
     *
     * @param request the request to add the form entity to.
     */
    public void toRequestEntity(Request request) {
        String form = toFormString();
        request.setMethod("POST");
        request.getHeaders().put(ContentTypeHeader.NAME, "application/x-www-form-urlencoded");
        request.getHeaders().put(ContentLengthHeader.NAME, form.length());
        request.getEntity().setString(form);
    }
}
