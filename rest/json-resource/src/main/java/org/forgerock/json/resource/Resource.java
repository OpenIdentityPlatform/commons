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
 * Copyright 2012-2015 ForgeRock AS.
 */

package org.forgerock.json.resource;

import java.util.LinkedHashMap;

import org.forgerock.json.fluent.JsonValue;

/**
 * A resource, comprising of a resource ID, a revision (etag), and its JSON
 * content.
 */
public final class Resource {

    /**
     * The name of the field which contains the resource ID in the JSON
     * representation.
     * <p>
     * <b>Note:</b> when encoding the resource ID as part of a resource's
     * content the field name {@link #FIELD_CONTENT_ID} should be used.
     */
    public static final String FIELD_ID = "id";

    /**
     * The name of the field which contains the resource version in the JSON
     * representation.
     * <p>
     * <b>Note:</b> when encoding the resource revision as part of a resource's
     * content the field name {@link #FIELD_CONTENT_REVISION} should be used.
     */
    public static final String FIELD_REVISION = "revision";

    /**
     * The name of the field in the resource content which contains the resource
     * ID. This field is semantically equivalent to {@link #FIELD_ID} and is
     * intended for use in cases where a commons REST API wishes to expose the
     * resource ID as part of the resource content.
     */
    public static final String FIELD_CONTENT_ID = "_id";

    /**
     * The name of the field in the resource content which contains the resource
     * revision. This field is semantically equivalent to
     * {@link #FIELD_REVISION} and is intended for use in cases where a commons
     * REST API wishes to expose the resource revision as part of the resource
     * content.
     */
    public static final String FIELD_CONTENT_REVISION = "_rev";

    /**
     * The name of the field which contains the resource content in the JSON
     * representation.
     */
    public static final String FIELD_CONTENT = "content";

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
        final int hash = id != null ? id.hashCode() : 17;
        return (hash * 31) + (revision != null ? revision.hashCode() : 0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        final JsonValue wrapper = new JsonValue(new LinkedHashMap<>(3));
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
