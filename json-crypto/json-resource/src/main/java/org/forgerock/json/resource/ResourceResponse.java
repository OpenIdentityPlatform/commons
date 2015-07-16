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

import org.forgerock.json.JsonValue;

/**
 * A resource, comprising of a resource ID, a revision (etag), and its JSON
 * content.
 */
public interface ResourceResponse extends Response {

    /**
     * The name of the field which contains the resource ID in the JSON
     * representation.
     * <p>
     * <b>Note:</b> when encoding the resource ID as part of a resource's
     * content the field name {@link #FIELD_CONTENT_ID} should be used.
     */
    String FIELD_ID = "id";

    /**
     * The name of the field which contains the resource version in the JSON
     * representation.
     * <p>
     * <b>Note:</b> when encoding the resource revision as part of a resource's
     * content the field name {@link #FIELD_CONTENT_REVISION} should be used.
     */
    String FIELD_REVISION = "revision";

    /**
     * The name of the field in the resource content which contains the resource
     * ID. This field is semantically equivalent to {@link #FIELD_ID} and is
     * intended for use in cases where a commons REST API wishes to expose the
     * resource ID as part of the resource content.
     */
    String FIELD_CONTENT_ID = "_id";

    /**
     * The name of the field in the resource content which contains the resource
     * revision. This field is semantically equivalent to
     * {@link #FIELD_REVISION} and is intended for use in cases where a commons
     * REST API wishes to expose the resource revision as part of the resource
     * content.
     */
    String FIELD_CONTENT_REVISION = "_rev";

    /**
     * The name of the field which contains the resource content in the JSON
     * representation.
     */
    String FIELD_CONTENT = "content";

    /**
     * Returns the JSON content of this resource.
     *
     * @return The JSON content of this resource.
     */
    JsonValue getContent();

    /**
     * Returns the ID of this resource, if applicable.
     *
     * @return The ID of this resource, or {@code null} if this resource does
     *         not have an ID.
     */
    String getId();

    /**
     * Returns the revision of this resource, if known.
     *
     * @return The revision of this resource, or {@code null} if the version is
     *         not known.
     */
    String getRevision();

    /**
     * Returns {@code true} if the provided object is a resource having the same
     * resource ID and revision as this resource.
     * <p>
     * {@inheritDoc}
     */
    @Override
    boolean equals(final Object obj);

    /**
     * Returns the hash code for this resource. Two resources are guaranteed to
     * have the same hash code if they share the same resource ID and revision.
     * <p>
     * {@inheritDoc}
     */
    @Override
    int hashCode();
}
