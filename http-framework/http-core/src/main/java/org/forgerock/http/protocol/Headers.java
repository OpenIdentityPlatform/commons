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

import static org.forgerock.http.header.HeaderFactory.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.forgerock.http.header.GenericHeader;
import org.forgerock.http.header.HeaderFactory;
import org.forgerock.http.header.MalformedHeaderException;

/**
 * Message headers, a case-insensitive multiple-value map.
 */
public class Headers implements Map<String, Object> {

    private final Map<String, Header> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

    /**
     * Constructs a {@code Headers} object that is case-insensitive for header names.
     */
    public Headers() { }

    /**
     * Defensive copy constructor.
     */
    Headers(final Headers headers) {
        // Force header re-creation
        for (Map.Entry<String, Header> entry : headers.asMapOfHeaders().entrySet()) {
            add(entry.getKey(), entry.getValue().getValues());
        }
    }

    /**
     * Rich-type friendly get method.
     * @param key The name of the header.
     * @return The header object.
     */
    @Override
    public Header get(Object key) {
        return headers.get(key);
    }

    /**
     * Gets the first value of the header, or null if the header does not exist.
     * @param key The name of the header.
     * @return The first header value.
     */
    public String getFirst(String key) {
        final Header header = headers.get(key);
        return header == null ? null : header.getFirstValue();
    }

    /**
     * Gets the first value of the header, or null if the header does not exist.
     * @param key The name of the header.
     * @return The first header value.
     */
    public String getFirst(Class<? extends Header> key) {
        final Header header = headers.get(getHeaderName(key));
        return header == null ? null : header.getFirstValue();
    }

    /**
     * Returns the specified {@link Header} or {code null} if the header is not included in the message.
     *
     * @param headerType The type of header.
     * @param <H> The type of header.
     * @return The header instance, or null if none exists.
     * @throws MalformedHeaderException When the header was not well formed, and so could not be parsed as
     * its richly-typed class.
     */
    public <H extends Header> H get(Class<H> headerType) throws MalformedHeaderException {
        final Header header = this.get(getHeaderName(headerType));
        if (header instanceof GenericHeader) {
            throw new MalformedHeaderException("Header value(s) are not well formed");
        }
        return headerType.cast(header);
    }

    private <H extends Header> String getHeaderName(Class<H> headerType) {
        final String headerName = HEADER_NAMES.get(headerType);
        if (headerName == null) {
            throw new IllegalArgumentException("Unknown header type: " + headerType);
        }
        return headerName;
    }

    /**
     * A script compatible putAll method that will accept {@code Header}, {@code String}, {@code Collection<String>}
     * and {@code String[]} values.
     * @param m A map of header names to values.
     */
    @Override
    public void putAll(Map<? extends String, ? extends Object> m) {
        for (Map.Entry<? extends String, ? extends Object> entry : m.entrySet()) {
            put(entry.getKey(), entry.getValue());
        }
    }

    /**
     * A script compatible put method that will accept a {@code Header}, {@code String}, {@code Collection<String>}
     * and {@code String[]} value.
     * @param key The name of the header.
     * @param value A {@code Header}, {@code String}, {@code Collection<String>} or {@code String[]}.
     * @return The previous {@code Header} value for this header, or null.
     */
    @Override
    public Header put(String key, Object value) {
        if (value == null) {
            return remove(key);
        }
        final HeaderFactory<?> factory = FACTORIES.get(key);
        if (value instanceof Header) {
            return putHeader(key, (Header) value, factory);
        } else if (factory != null) {
            return putUsingFactory(key, value, factory);
        } else {
            return putGenericHeader(key, value);
        }
    }

    private Header putGenericHeader(String key, Object value) {
        if (value instanceof String) {
            return putGenericString(key, (String) value);
        } else if (value instanceof List) {
            return putGenericList(key, (List<?>) value);
        } else if (value instanceof Collection) {
            return putGenericList(key, new ArrayList<>((Collection<?>) value));
        } else if (value.getClass().isArray()) {
            return putGenericList(key, Arrays.asList((Object[]) value));
        }
        throw new IllegalArgumentException("Cannot put object for key '" + key + "': " + value);
    }

    private Header putHeader(String key, Header header, HeaderFactory<?> factory) {
        if (!hasAnyValue(header)) {
            return remove(key);
        }
        if (HEADER_NAMES.containsValue(key) && !HEADER_NAMES.get(header.getClass()).equals(key)) {
            if (header instanceof GenericHeader) {
                return putUsingFactory(key, header.getValues(), factory);
            }
            throw new IllegalArgumentException("Header object of incorrect type for header " + key);
        }
        return headers.put(key, header);
    }

    private boolean hasAnyValue(Header header) {
        boolean hasValue = false;
        for (String s : header.getValues()) {
            if (s != null) {
                hasValue = true;
                break;
            }
        }
        return hasValue;
    }

    @SuppressWarnings("unchecked")
    private Header putGenericList(String key, List<?> value) {
        if (value.isEmpty()) {
            return remove(key);
        }
        for (Object o : value) {
            if (!(o instanceof String)) {
                throw new IllegalArgumentException("Collections must be of strings");
            }
        }
        return headers.put(key, new GenericHeader(key, (List<String>) value));
    }

    private Header putGenericString(String key, String value) {
        return headers.put(key, new GenericHeader(key, value));
    }

    private Header putUsingFactory(String key, Object value, HeaderFactory<?> factory) {
        final Header parsed;
        try {
            parsed = factory.parse(value);
        } catch (MalformedHeaderException e) {
            if (value instanceof Header) {
                value = ((Header) value).getValues();
            }
            return putGenericHeader(key, value);
        }
        if (parsed == null) {
            return remove(key);
        }
        return headers.put(key, parsed);
    }

    /**
     * Rich-type friendly remove method. Removes the {@code Header} object for the given header name.
     * @param key The header name.
     * @return The header value before removal, or null.
     */
    @Override
    public Header remove(Object key) {
        return headers.remove(key);
    }

    /**
     * A put method to add a particular {@code Header} instance. Will overwrite any existing value for this
     * header name.
     * @param header The header instance.
     * @return The previous {@code Header} value for the header with the same name, or null.
     */
    public Header put(Header header) {
        return put(header.getName(), header);
    }

    /**
     * An add method to add a particular {@code Header} instance. Existing values for the header will be added to.
     *
     * @param header The header instance.
     */
    public void add(Header header) {
        add(header.getName(), header);
    }

    /**
     * A script compatible add method that will accept a {@code Header}, {@code String}, {@code Collection<String>}
     * and {@code String[]} value. Existing values for the header will be added to.
     * @param key The name of the header.
     * @param value A {@code Header}, {@code String}, {@code Collection<String>} or {@code String[]}.
     */
    @SuppressWarnings("unchecked")
    public void add(String key, Object value) {
        if (value == null) {
            return;
        }
        List<String> values = containsKey(key) ? new ArrayList<>(get(key).getValues()) : new ArrayList<String>();
        if (value instanceof Header) {
            for (String s : ((Header) value).getValues()) {
                addNonNullStringValue(values, s);
            }
        } else if (value instanceof String) {
            values.add((String) value);
        } else if (value instanceof Collection) {
            final Collection<String> collection = (Collection<String>) value;
            for (String s : collection) {
                addNonNullStringValue(values, s);
            }
        } else if (value.getClass().isArray()) {
            String[] array = (String[]) value;
            for (String s : array) {
                addNonNullStringValue(values, s);
            }
        } else {
            throw new IllegalArgumentException("Cannot add values for key '" + key + "': " + value);
        }
        if (values.isEmpty()) {
            return;
        }
        final HeaderFactory<?> factory = FACTORIES.get(key);
        if (factory != null) {
            Header parsed;
            try {
                parsed = factory.parse(values);
            } catch (MalformedHeaderException e) {
                parsed = new GenericHeader(key, values);
            }
            if (parsed == null) {
                return;
            }
            headers.put(key, parsed);
        } else {
            headers.put(key, new GenericHeader(key, values));
        }
    }

    private void addNonNullStringValue(List<String> values, String s) {
        if (s != null) {
            values.add(s);
        }
    }

    /**
     * A script compatible addAll method that will accept a {@code Header}, {@code String}, {@code Collection<String>}
     * and {@code String[]} value. Existing values for the headers will be added to.
     * @param map A map of header names to values.
     */
    public void addAll(Map<? extends String, ? extends Object> map) {
        for (Map.Entry<? extends String, ? extends Object> entry : map.entrySet()) {
            add(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public int size() {
        return headers.size();
    }

    @Override
    public boolean isEmpty() {
        return headers.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return headers.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return headers.containsValue(value);
    }

    @Override
    public void clear() {
        headers.clear();
    }

    @Override
    public Set<String> keySet() {
        return headers.keySet();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Collection<Object> values() {
        return (Collection) headers.values();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Set<Entry<String, Object>> entrySet() {
        return (Set) headers.entrySet();
    }

    /**
     * The {@code Headers} class extends {@code Map<String, Object>} to support flexible parameters in scripting. This
     * method allows access to the underlying {@code Map<String, Header>}.
     * @return The map of header names to {@link Header} objects.
     */
    public Map<String, Header> asMapOfHeaders() {
        return headers;
    }

    /**
     * Returns a copy of these headers as a multi-valued map of strings. Changes to the returned map will not be
     * reflected in these headers, nor will changes in these headers be reflected in the returned map.
     *
     * @return a copy of these headers as a multi-valued map of strings.
     */
    public Map<String, List<String>> copyAsMultiMapOfStrings() {
        Map<String, List<String>> result = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (Header header : headers.values()) {
            result.put(header.getName(), new ArrayList<>(header.getValues()));
        }
        return result;
    }

}
