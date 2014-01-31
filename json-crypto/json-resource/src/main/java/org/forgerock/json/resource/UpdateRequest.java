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
 * Copyright 2012-2013 ForgeRock AS.
 */

package org.forgerock.json.resource;

import java.util.List;

import org.forgerock.json.fluent.JsonPointer;
import org.forgerock.json.fluent.JsonValue;

/**
 * A request to update a JSON resource by replacing its existing content with
 * new content.
 */
public interface UpdateRequest extends Request {

    /**
     * The name of the field which contains the resource content in the JSON
     * representation.
     */
    public static final String FIELD_CONTENT = "content";

    /**
     * The name of the field which contains the resource version in the JSON
     * representation.
     */
    public static final String FIELD_REVISION = "revision";

    /**
     * {@inheritDoc}
     */
    <R, P> R accept(RequestVisitor<R, P> v, P p);

    /**
     * {@inheritDoc}
     */
    @Override
    UpdateRequest addField(JsonPointer... fields);

    /**
     * {@inheritDoc}
     */
    @Override
    UpdateRequest addField(String... fields);

    /**
     * {@inheritDoc}
     */
    @Override
    List<JsonPointer> getFields();

    /**
     * Returns the new content of the JSON resource to be replaced.
     *
     * @return The new content of the JSON resource to be replaced.
     * @deprecated Use {@link UpdateRequest#getContent} instead.
     */
    @Deprecated
    JsonValue getNewContent();

    /**
     * Returns the content of the JSON resource to be replaced.
     *
     * @return The content of the JSON resource to be replaced.
     */
    JsonValue getContent();

    /**
     * {@inheritDoc}
     */
    @Override
    RequestType getRequestType();

    /**
     * {@inheritDoc}
     */
    @Override
    String getResourceName();

    /**
     * {@inheritDoc}
     */
    @Override
    ResourceName getResourceNameObject();

    /**
     * Returns the expected version information associated with the JSON
     * resource to be updated. Version information can be used in order to
     * implement multi-version concurrency control (MVCC).
     * <p>
     * The returned version information may be {@code null} indicating that the
     * client does not care if the resource has been modified concurrently. If
     * the version information is non-{@code null}, and it does not match the
     * current version of the targeted JSON resource, then the update request
     * will be rejected by the provider.
     *
     * @return The expected version information associated with the JSON
     *         resource to be updated.
     */
    String getRevision();

    /**
     * Sets the new content of the JSON resource to be replaced.
     *
     * @param content
     *            The new content of the JSON resource to be replaced.
     * @return This update request.
     * @throws UnsupportedOperationException
     *             If this update request does not permit changes to the
     *             content.
     * @deprecated Use {@link UpdateRequest#setContent} instead.
     */
    @Deprecated
    UpdateRequest setNewContent(JsonValue content);

    /**
     * Sets the content of the JSON resource to be replaced.
     *
     * @param content
     *            The content of the JSON resource to be replaced.
     * @return This update request.
     * @throws UnsupportedOperationException
     *             If this update request does not permit changes to the
     *             content.
     */
    UpdateRequest setContent(JsonValue content);

    /**
     * {@inheritDoc}
     */
    @Override
    Request setResourceName(ResourceName name);

    /**
     * {@inheritDoc}
     */
    @Override
    UpdateRequest setResourceName(String name);

    /**
     * Sets the expected version information associated with the JSON resource
     * to be updated. Version information can be used in order to implement
     * multi-version concurrency control (MVCC).
     * <p>
     * The provided version information may be {@code null} indicating that the
     * client does not care if the resource has been modified concurrently. If
     * the version information is non-{@code null}, and it does not match the
     * current version of the targeted JSON resource, then the update request
     * will be rejected by the provider.
     *
     * @param version
     *            The expected version information associated with the JSON
     *            resource to be updated.
     * @return This patch request.
     * @throws UnsupportedOperationException
     *             If this update request does not permit changes to the version
     *             information.
     */
    UpdateRequest setRevision(String version);
}
