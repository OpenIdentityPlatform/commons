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
 * Copyright © 2012 ForgeRock AS. All rights reserved.
 */

package org.forgerock.json.resource;

import java.util.List;

import org.forgerock.json.fluent.JsonPointer;

/**
 * A request to update a JSON resource by applying a set of changes to its
 * existing content.
 */
public interface PatchRequest extends Request {

    /**
     * {@inheritDoc}
     */
    @Override
    PatchRequest addField(JsonPointer... fields);

    /**
     * {@inheritDoc}
     */
    @Override
    PatchRequest addField(String... fields);

    /**
     * {@inheritDoc}
     */
    @Override
    String getResourceName();

    /**
     * {@inheritDoc}
     */
    @Override
    List<JsonPointer> getFieldFilters();

    /**
     * Returns the patch which should be applied to the JSON resource.
     *
     * @return The patch which should be applied to the JSON resource.
     */
    Patch getPatch();

    /**
     * Returns the expected version information associated with the JSON
     * resource to be patched. Version information can be used in order to
     * implement multi-version concurrency control (MVCC).
     * <p>
     * The returned version information may be {@code null} indicating that the
     * client does not care if the resource has been modified concurrently. If
     * the version information is non-{@code null}, and it does not match the
     * current version of the targeted JSON resource, then the patch request
     * will be rejected by the provider.
     *
     * @return The expected version information associated with the JSON
     *         resource to be patched.
     */
    String getRevision();

    /**
     * {@inheritDoc}
     */
    @Override
    PatchRequest setResourceName(String name);

    /**
     * Sets the patch which should be applied to the JSON resource.
     *
     * @param changes
     *            The patch which should be applied to the JSON resource.
     * @return This patch request.
     * @throws UnsupportedOperationException
     *             If this patch request does not permit changes to the patch.
     */
    PatchRequest setPatch(Patch changes);

    /**
     * Sets the expected version information associated with the JSON resource
     * to be patched. Version information can be used in order to implement
     * multi-version concurrency control (MVCC).
     * <p>
     * The provided version information may be {@code null} indicating that the
     * client does not care if the resource has been modified concurrently. If
     * the version information is non-{@code null}, and it does not match the
     * current version of the targeted JSON resource, then the patch request
     * will be rejected by the provider.
     *
     * @param version
     *            The expected version information associated with the JSON
     *            resource to be patched.
     * @return This patch request.
     * @throws UnsupportedOperationException
     *             If this patch request does not permit changes to the version
     *             information.
     */
    PatchRequest setRevision(String version);
}
