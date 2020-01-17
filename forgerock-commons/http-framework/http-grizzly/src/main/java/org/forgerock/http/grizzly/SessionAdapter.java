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
 * Copyright 2016 ForgeRock AS.
 */
package org.forgerock.http.grizzly;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.glassfish.grizzly.http.server.Session;

/**
 * Exposes the session managed by Grizzly as an exchange session.
 */
final class SessionAdapter implements org.forgerock.http.session.Session {

    private final Session session;

    SessionAdapter(Session session) {
        this.session = session;
    }

    @Override
    public int size() {
        return session.attributes().size();
    }

    @Override
    public boolean isEmpty() {
        return session.attributes().isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return session.attributes().containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return session.attributes().containsValue(value);
    }

    @Override
    public Object get(Object key) {
        return session.attributes().get(key);
    }

    @Override
    public Object put(String key, Object value) {
        return session.attributes().put(key, value);
    }

    @Override
    public Object remove(Object key) {
        return session.attributes().remove(key);
    }

    @Override
    public void putAll(Map<? extends String, ? extends Object> m) {
        session.attributes().putAll(m);
    }

    @Override
    public void clear() {
        session.attributes().clear();
    }

    @Override
    public Set<String> keySet() {
        return session.attributes().keySet();
    }

    @Override
    public Collection<Object> values() {
        return session.attributes().values();
    }

    @Override
    public Set<Map.Entry<String, Object>> entrySet() {
        return session.attributes().entrySet();
    }

    @Override
    public void save(org.forgerock.http.protocol.Response response) throws IOException {
        // Nothing to do when using Session
    }
}