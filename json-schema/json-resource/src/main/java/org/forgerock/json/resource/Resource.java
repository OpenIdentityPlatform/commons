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

import org.forgerock.json.fluent.JsonValue;

/**
 * A JSON resource, comprising of a resource ID, a revision (etag), and its JSON
 * content.
 */
public final class Resource {

    // TODO: should we have a getter for the endpoint as well? I think the
    // endpoint is an extrinsic property of a resource.

    private final JsonValue content;
    private final String id;
    private final String revision;

    /**
     * Creates a new JSON resource.
     *
     * @param id
     *            The JSON resource ID, if applicable.
     * @param revision
     *            The JSON resource version, if known.
     * @param content
     *            The JSON resource content.
     */
    public Resource(final String id, final String revision, final JsonValue content) {
        this.id = id;
        this.revision = revision;
        this.content = content;
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
     * Returns the ID of this JSON resource, if applicable.
     *
     * @return The ID of this JSON resource, or {@code null} if this resource
     *         does not have an ID.
     */
    public String getId() {
        return id;
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
}
