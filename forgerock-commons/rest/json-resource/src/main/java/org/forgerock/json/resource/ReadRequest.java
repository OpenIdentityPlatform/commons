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
 * Copyright © 2012-2015 ForgeRock AS. All rights reserved.
 */

package org.forgerock.json.resource;

import java.util.List;
import java.util.Map;

import org.forgerock.http.routing.Version;
import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.util.i18n.PreferredLocales;

/**
 * A request to read a single identified JSON resource.
 */
public interface ReadRequest extends Request {

    @Override
    <R, P> R accept(final RequestVisitor<R, P> v, final P p);


    @Override
    ReadRequest addField(JsonPointer... fields);


    @Override
    ReadRequest addField(String... fields);


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

    @Override
    ReadRequest setAdditionalParameter(String name, String value) throws BadRequestException;

    @Override
    ReadRequest setPreferredLocales(PreferredLocales preferredLocales);

    @Override
    ReadRequest setResourcePath(ResourcePath path);

    @Override
    ReadRequest setResourcePath(String path);

    @Override
    ReadRequest setResourceVersion(Version resourceVersion);

    @Override
    JsonValue toJsonValue();
}
