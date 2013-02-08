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
 * Copyright 2012 ForgeRock AS. All rights reserved.
 */

package org.forgerock.json.resource;

import java.util.LinkedHashMap;

import org.forgerock.json.fluent.JsonValue;

/**
 * A resource, comprising of a resource ID, a revision (etag), and its JSON
 * content.
 */
public final class Resource {

    private final JsonValue content;
    private final String id;
    private final String revision;

    /**
     * Creates a new resource.
     *
     * @param id
     *            The resource ID if applicable otherwise {@code null}.
     * @param revision
     *            The resource version, if known.
     * @param content
     *            The resource content.
     */
    public Resource(final String id, final String revision, final JsonValue content) {
        this.id = id;
        this.revision = revision;
        this.content = content;
    }

    /**
     * Returns {@code true} if the provided object is a resource having the same
     * resource ID and revision as this resource.
     * <p>
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        } else if (obj instanceof Resource) {
            final Resource that = (Resource) obj;
            return isEqual(id, that.id) && isEqual(revision, that.revision);
        } else {
            return false;
        }
    }

    /**
     * Returns the JSON content of this resource.
     *
     * @return The JSON content of this resource.
     */
    public JsonValue getContent() {
        return content;
    }

    /**
     * Returns the ID of this resource, if applicable.
     *
     * @return The ID of this resource, or {@code null} if this resource does
     *         not have an ID.
     */
    public String getId() {
        return id;
    }

    /**
     * Returns the revision of this resource, if known.
     *
     * @return The revision of this resource, or {@code null} if the version is
     *         not known.
     */
    public String getRevision() {
        return revision;
    }

    /**
     * Returns the hash code for this resource. Two resources are guaranteed to
     * have the same hash code if they share the same resource ID and revision.
     * <p>
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        int hash = 17;
        hash = hash * 31 + id != null ? id.hashCode() : 0;
        hash = hash * 31 + revision != null ? revision.hashCode() : 0;
        return hash;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        final JsonValue wrapper = new JsonValue(new LinkedHashMap<String, Object>(3));
        wrapper.add("id", id);
        wrapper.add("rev", revision);
        wrapper.add("content", content);
        return wrapper.toString();
    }

    private boolean isEqual(final String s1, final String s2) {
        if (s1 == s2) {
            return true;
        } else if (s1 == null || s2 == null) {
            return false;
        } else {
            return s1.equals(s2);
        }
    }

}
