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

import java.util.List;
import java.util.Map;

import org.forgerock.http.routing.Version;
import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.util.i18n.PreferredLocales;

/**
 * A request to delete a JSON resource.
 */
public interface DeleteRequest extends Request {

    /**
     * The name of the field which contains the resource version in the JSON representation.
     */
    String FIELD_REVISION = "revision";


    @Override
    <R, P> R accept(final RequestVisitor<R, P> v, final P p);


    @Override
    DeleteRequest addField(JsonPointer... fields);


    @Override
    DeleteRequest addField(String... fields);


    @Override
    String getAdditionalParameter(String name);


    @Override
    Map<String, String> getAdditionalParameters();


    @Override
    List<JsonPointer> getFields();


    @Override
    PreferredLocales getPreferredLocales();


    @Override
    RequestType getRequestType();

    @Override
    String getResourcePath();

    @Override
    ResourcePath getResourcePathObject();

    @Override
    Version getResourceVersion();

    /**
     * Returns the expected version information associated with the JSON resource to be deleted. Version information can
     * be used in order to implement multi-version concurrency control (MVCC).
     * <p>
     * The returned version information may be {@code null} indicating that the client does not care if the resource has
     * been modified concurrently. If the version information is non-{@code null}, and it does not match the current
     * version of the targeted JSON resource, then the delete request will be rejected by the provider.
     *
     * @return The expected version information associated with the JSON resource to be deleted.
     */
    String getRevision();

    @Override
    DeleteRequest setAdditionalParameter(String name, String value) throws BadRequestException;

    @Override
    DeleteRequest setPreferredLocales(PreferredLocales preferredLocales);

    @Override
    DeleteRequest setResourcePath(String path);

    @Override
    DeleteRequest setResourcePath(ResourcePath path);

    @Override
    DeleteRequest setResourceVersion(Version resourceVersion);

    /**
     * Sets the expected version information associated with the JSON resource to be deleted. Version information can be
     * used in order to implement multi-version concurrency control (MVCC).
     * <p>
     * The provided version information may be {@code null} indicating that the client does not care if the resource has
     * been modified concurrently. If the version information is non-{@code null}, and it does not match the current
     * version of the targeted JSON resource, then the delete request will be rejected by the provider.
     *
     * @param version
     *         The expected version information associated with the JSON resource to be deleted.
     * @return This delete request.
     * @throws UnsupportedOperationException
     *         If this delete request does not permit changes to the version information.
     */
    DeleteRequest setRevision(String version);

    @Override
    JsonValue toJsonValue();
}
