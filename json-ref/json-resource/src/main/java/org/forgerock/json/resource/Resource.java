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
 * A JSON resource, comprising of a resource ID, a revision (etag), and its JSON
 * content.
 */
public final class Resource {

    // TODO: should we have a getter for the endpoint as well? I think the
    // endpoint is an extrinsic property of a resource.

    private final JsonValue content;
    private final String resourceName;
    private final String revision;

    /**
     * Creates a new JSON resource.
     *
     * @param resourceName
     *            The resource name.
     * @param revision
     *            The JSON resource version, if known.
     * @param content
     *            The JSON resource content.
     */
    public Resource(final String resourceName, final String revision, final JsonValue content) {
        this.resourceName = resourceName;
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
            return resourceName.equals(resourceName) && isEqual(revision, that.revision);
        } else {
            return false;
        }
    }

    /**
     * Returns the content of this JSON resource.
     *
     * @return The content of this JSON resource.
     */
    public JsonValue getContent() {
        return content;
    }

    /**
     * Returns the name of this JSON resource.
     *
     * @return The name of this JSON resource.
     */
    public String getResourceName() {
        return resourceName;
    }

    /**
     * Returns the version of this JSON resource, if known.
     *
     * @return The version of this JSON resource, or {@code null} if version is
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
        hash = hash * 31 + resourceName.hashCode();
        hash = hash * 31 + revision != null ? revision.hashCode() : 0;
        return hash;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        final JsonValue wrapper = new JsonValue(new LinkedHashMap<String, Object>(3));
        wrapper.add("id", resourceName);
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
