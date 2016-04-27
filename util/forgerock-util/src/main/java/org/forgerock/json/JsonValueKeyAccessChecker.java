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
 * information: "Portions Copyrighted [year] [name of copyright owner]".
 *
 * Copyright 2013-2015 ForgeRock AS.
 */

package org.forgerock.json;

import java.io.File;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Subclass of {@link JsonValue} that checks all keys are accessed.
 */
public class JsonValueKeyAccessChecker extends JsonValue {

    private final JsonValue delegate;
    private final Set<String> accessedKeyNames = new HashSet<>();
    private final Map<JsonPointer, JsonValueKeyAccessChecker> subCheckers = new HashMap<>();

    /**
     * Constructs a {@link JsonValueKeyAccessChecker}.
     *
     * @param delegate The delegate to check
     */
    public JsonValueKeyAccessChecker(JsonValue delegate) {
        super(null);
        this.delegate = delegate;
        if (delegate != null) {
            for (JsonValue value : delegate) {
                wrap(value);
            }
        }
    }

    private JsonValue access(JsonValue val) {
        String keyName = val.getPointer().leaf();
        if (keyName != null) {
            accessedKeyNames.add(keyName);
        }
        return val;
    }

    private JsonValue wrap(JsonValue val) {
        if (val == this.delegate) {
            return this;
        } else if (val.isMap() || val.isList()) {
            // wrap the list and map to ensure the key access check is propagated
            JsonValueKeyAccessChecker checker = subCheckers.get(val.getPointer());
            if (checker == null) {
                checker = new JsonValueKeyAccessChecker(val);
                subCheckers.put(val.getPointer(), checker);
            }
            return checker;
        }
        // there is no key access to check, just return the value itself
        return val;
    }

    /** {@inheritDoc} */
    @Override
    public JsonValue recordKeyAccesses() {
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public void verifyAllKeysAccessed() {
        StringBuilder errors = new StringBuilder();
        verifyAllKeysAccessed(errors);
        if (errors.length() > 0) {
            throw new JsonException(errors.toString());
        }
    }

    private void verifyAllKeysAccessed(StringBuilder errors) {
        final Set<String> unaccessedKeys = isList() ? Collections.<String>emptySet()
                            : new TreeSet<>(this.delegate.keys());
        unaccessedKeys.removeAll(this.accessedKeyNames);
        if (!unaccessedKeys.isEmpty()) {
            if (errors.length() > 0) {
                errors.append("\n");
            }
            errors.append(getPointer()).append(": ").append("Unused keys: " + unaccessedKeys);
        }

        for (JsonValueKeyAccessChecker subChecker : this.subCheckers.values()) {
            // do not report unaccessed keys located under an already reported unaccessed key
            if (!unaccessedKeys.contains(subChecker.getPointer().leaf())) {
                subChecker.verifyAllKeysAccessed(errors);
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public JsonValue add(final int index, final Object object) {
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc} */
    @Override
    public JsonValue add(final JsonPointer pointer, final Object object) {
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc} */
    @Override
    public JsonValue add(final Object object) {
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc} */
    @Override
    public JsonValue add(final String key, final Object object) {
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc} */
    @Override
    public JsonValue addPermissive(final JsonPointer pointer, final Object object) {
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc} */
    @Override
    public void applyTransformers() {
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc} */
    @Override
    public Boolean asBoolean() {
        return this.delegate.asBoolean();
    }

    /** {@inheritDoc} */
    @Override
    public Charset asCharset() {
        return this.delegate.asCharset();
    }

    /** {@inheritDoc} */
    @Override
    public Double asDouble() {
        return this.delegate.asDouble();
    }

    /** {@inheritDoc} */
    @Override
    public <T extends Enum<T>> T asEnum(final Class<T> type) {
        return this.delegate.asEnum(type);
    }

    /** {@inheritDoc} */
    @Override
    public File asFile() {
        return this.delegate.asFile();
    }

    /** {@inheritDoc} */
    @Override
    public Integer asInteger() {
        return this.delegate.asInteger();
    }

    /** {@inheritDoc} */
    @Override
    public List<Object> asList() {
        return this.delegate.asList();
    }

    /** {@inheritDoc} */
    @Override
    public <E> List<E> asList(final Class<E> type) {
        return this.delegate.asList(type);
    }

    /** {@inheritDoc} */
    @Override
    public Long asLong() {
        return this.delegate.asLong();
    }

    /** {@inheritDoc} */
    @Override
    public Map<String, Object> asMap() {
        return this.delegate.asMap();
    }

    /** {@inheritDoc} */
    @Override
    public Number asNumber() {
        return this.delegate.asNumber();
    }

    /** {@inheritDoc} */
    @Override
    public Pattern asPattern() {
        return this.delegate.asPattern();
    }

    /** {@inheritDoc} */
    @Override
    public JsonPointer asPointer() {
        return this.delegate.asPointer();
    }

    /** {@inheritDoc} */
    @Override
    public String asString() {
        return this.delegate.asString();
    }

    /** {@inheritDoc} */
    @Override
    public URI asURI() {
        return this.delegate.asURI();
    }

    /** {@inheritDoc} */
    @Override
    public UUID asUUID() {
        return this.delegate.asUUID();
    }

    /** {@inheritDoc} */
    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc} */
    @Override
    public JsonValue clone() {
        return wrap(this.delegate.clone());
    }

    /** {@inheritDoc} */
    @Override
    public boolean contains(final Object object) {
        return this.delegate.contains(object);
    }

    /** {@inheritDoc} */
    @Override
    public JsonValue copy() {
        return wrap(this.delegate.copy());
    }

    /** {@inheritDoc} */
    @Override
    public JsonValue defaultTo(final Object object) {
        return wrap(this.delegate.defaultTo(object));
    }

    /** {@inheritDoc} */
    @Override
    public JsonValue expect(final Class<?> type) {
        return wrap(this.delegate.expect(type));
    }

    /** {@inheritDoc} */
    @Override
    public JsonValue get(final int index) {
        return wrap(access(this.delegate.get(index)));
    }

    /** {@inheritDoc} */
    @Override
    public JsonValue get(final JsonPointer pointer) {
        return wrap(access(this.delegate.get(pointer)));
    }

    /** {@inheritDoc} */
    @Override
    public JsonValue get(final String key) {
        return wrap(access(this.delegate.get(key)));
    }

    /** {@inheritDoc} */
    @Override
    public Object getObject() {
        return this.delegate.getObject();
    }

    /** {@inheritDoc} */
    @Override
    public JsonPointer getPointer() {
        return this.delegate.getPointer();
    }

    /** {@inheritDoc} */
    @Override
    public List<JsonTransformer> getTransformers() {
        return this.delegate.getTransformers();
    }

    /** {@inheritDoc} */
    @Override
    public boolean isBoolean() {
        return this.delegate.isBoolean();
    }

    /** {@inheritDoc} */
    @Override
    public boolean isDefined(final String key) {
        return this.delegate.isDefined(key);
    }

    /** {@inheritDoc} */
    @Override
    public boolean isList() {
        return this.delegate.isList();
    }

    /** {@inheritDoc} */
    @Override
    public boolean isMap() {
        return this.delegate.isMap();
    }

    /** {@inheritDoc} */
    @Override
    public boolean isNull() {
        return this.delegate.isNull();
    }

    /** {@inheritDoc} */
    @Override
    public boolean isNumber() {
        return this.delegate.isNumber();
    }

    /** {@inheritDoc} */
    @Override
    public boolean isString() {
        return this.delegate.isString();
    }

    /** {@inheritDoc} */
    @Override
    public Iterator<JsonValue> iterator() {
        final Iterator<JsonValue> delegate = this.delegate.iterator();
        return new Iterator<JsonValue>() {

            @Override
            public boolean hasNext() {
                return delegate.hasNext();
            }

            @Override
            public JsonValue next() {
                return wrap(access(delegate.next()));
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    /** {@inheritDoc} */
    @Override
    public Set<String> keys() {
        return this.delegate.keys();
    }

    /** {@inheritDoc} */
    @Override
    public JsonValue put(final int index, final Object object) {
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc} */
    @Override
    public JsonValue put(final JsonPointer pointer, final Object object) {
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc} */
    @Override
    public JsonValue put(final String key, final Object object) {
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc} */
    @Override
    public JsonValue putPermissive(final JsonPointer pointer, final Object object) {
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc} */
    @Override
    public void remove(final int index) {
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc} */
    @Override
    public void remove(final JsonPointer pointer) {
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc} */
    @Override
    public void remove(final String key) {
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc} */
    @Override
    public JsonValue required() {
        return wrap(this.delegate.required());
    }

    /** {@inheritDoc} */
    @Override
    public void setObject(final Object object) {
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc} */
    @Override
    public int size() {
        return this.delegate.size();
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object obj) {
        return this.delegate.equals(obj);
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return this.delegate.hashCode();
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return this.delegate.toString();
    }
}
