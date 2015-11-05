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

package org.forgerock.http.header;

import static java.util.Collections.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.forgerock.http.protocol.Header;

/**
 * Creates instances of {@link Header} classes from String representation.
 * @param <H> The type of {@link Header} produced by the factory.
 */
public abstract class HeaderFactory<H extends Header> {
    /**
     * A map of {@link Header} types to the names of the headers they
     * implement.
     */
    public static final Map<Class<? extends Header>, String> HEADER_NAMES = unmodifiableMap(
            new HashMap<Class<? extends Header>, String>() {
                {
                    put(AcceptApiVersionHeader.class, AcceptApiVersionHeader.NAME);
                    put(AcceptLanguageHeader.class, AcceptLanguageHeader.NAME);
                    put(ConnectionHeader.class, ConnectionHeader.NAME);
                    put(ContentApiVersionHeader.class, ContentApiVersionHeader.NAME);
                    put(ContentEncodingHeader.class, ContentEncodingHeader.NAME);
                    put(ContentLengthHeader.class, ContentLengthHeader.NAME);
                    put(ContentTypeHeader.class, ContentTypeHeader.NAME);
                    put(CookieHeader.class, CookieHeader.NAME);
                    put(LocationHeader.class, LocationHeader.NAME);
                    put(SetCookieHeader.class, SetCookieHeader.NAME);
                    put(TransactionIdHeader.class, TransactionIdHeader.NAME);
                    put(WarningHeader.class, WarningHeader.NAME);
                }
            });

    /**
     * A map of header names to known {@code HeaderFactory} implementations.
     */
    public static final Map<String, HeaderFactory<?>> FACTORIES = unmodifiableMap(
            new TreeMap<String, HeaderFactory<?>>(String.CASE_INSENSITIVE_ORDER) {
                {
                    put(AcceptApiVersionHeader.NAME, new AcceptApiVersionHeader.Factory());
                    put(AcceptLanguageHeader.NAME, new AcceptLanguageHeader.Factory());
                    put(ConnectionHeader.NAME, new ConnectionHeader.Factory());
                    put(ContentApiVersionHeader.NAME, new ContentApiVersionHeader.Factory());
                    put(ContentEncodingHeader.NAME, new ContentEncodingHeader.Factory());
                    put(ContentLengthHeader.NAME, new ContentLengthHeader.Factory());
                    put(ContentTypeHeader.NAME, new ContentTypeHeader.Factory());
                    put(CookieHeader.NAME, new CookieHeader.Factory());
                    put(LocationHeader.NAME, new LocationHeader.Factory());
                    put(SetCookieHeader.NAME, new SetCookieHeader.Factory());
                    put(TransactionIdHeader.NAME, new TransactionIdHeader.Factory());
                    put(WarningHeader.NAME, new WarningHeader.Factory());
                    if (size() != HEADER_NAMES.size()) {
                        throw new IllegalStateException("Misconfigured maps");
                    }
                }

                @Override
                public HeaderFactory<?> put(String key, HeaderFactory<?> value) {
                    if (!HEADER_NAMES.containsValue(key)) {
                        throw new IllegalStateException("Misconfigured maps");
                    }
                    return super.put(key, value);
                }
            });
    /**
     * Create a header instance from String representation, which may contain
     * multiple values if the header supports them.
     *
     * @param value The string representation.
     * @return The parsed header.
     * @throws MalformedHeaderException When the value cannot be parsed.
     */
    protected abstract H parse(String value) throws MalformedHeaderException;

    /**
     * Create a header instance from a list of String representations, each of
     * which may in turn contain multiple values if the header supports them.
     * @param values The string representations.
     * @return The parsed header.
     * @throws MalformedHeaderException When the value cannot be parsed.
     */
    protected abstract H parse(List<String> values) throws MalformedHeaderException;

    /**
     * Create a header instance from a provided object representation.
     * <p>
     * Subclasses may wish to override this method in order to support other
     * types of value.
     *
     * @param value An object representation - may be a string, a collection
     *              of strings, an array of strings, an instance of {@code Header},
     *              or some other object type supported by the subclass.
     * @return The parsed header.
     * @throws MalformedHeaderException When the value cannot be parsed.
     */
    public H parse(Object value) throws MalformedHeaderException {
        try {
            if (value instanceof Header) {
                return parse(new ArrayList<>(((Header) value).getValues()));
            } else if (value instanceof String) {
                final String s = (String) value;
                return s.isEmpty() ? null : parse(s);
            } else if (value instanceof List) {
                return parse((List<String>) value);
            } else if (value.getClass().isArray()) {
                return parse(Arrays.asList((String[]) value));
            }
        } catch (RuntimeException e) {
            throw new MalformedHeaderException("Could not parse header", e);
        }
        throw new IllegalArgumentException("Cannot parse header value from type: " + value);
    }
}
