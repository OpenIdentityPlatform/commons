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
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2013 ForgeRock AS.
 */
package org.forgerock.json.resource.api;

import static org.forgerock.json.fluent.JsonValue.field;
import static org.forgerock.json.fluent.JsonValue.object;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.AbstractRequestHandler;
import org.forgerock.json.resource.NotSupportedException;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResultHandler;
import org.forgerock.json.resource.ServerContext;

@SuppressWarnings("javadoc")
public final class Api {
    private Api() {
        // Nothing to do.
    }

    private static Object apiToJson(final ApiDescriptor api) {
        // @formatter:off
        return object(
                field("urn", String.valueOf(api.getUrn())),
                field("name", String.valueOf(api.getUrn().getName())),
                field("version", String.valueOf(api.getUrn().getVersion())),
                field("description", api.getDescription()),
                field("relations", relationsToJson(api.getRelations())),
                field("resources", resourcesToJson(api.getResources()))
        );
        // @formatter:on
    }

    private static Object resourcesToJson(final Set<ResourceDescriptor> resources) {
        final List<Object> json = new ArrayList<Object>(resources.size());
        for (final ResourceDescriptor resource : resources) {
            // @formatter:off
            json.add(object(
                    field("urn", String.valueOf(resource.getUrn())),
                    field("name", String.valueOf(resource.getUrn().getName())),
                    field("version", String.valueOf(resource.getUrn().getVersion())),
                    field("description", resource.getDescription()),
                    field("parent", String.valueOf(resource.getParentUrn())),
                    field("actions", actionsToJson(resource.getActions())),
                    field("relations", relationsToJson(resource.getRelations()))
            ));
            // @formatter:on
        }
        return json;
    }

    private static Object actionsToJson(final Set<ActionDescriptor> actions) {
        final List<Object> json = new ArrayList<Object>(actions.size());
        for (final ActionDescriptor action : actions) {
            // @formatter:off
            json.add(object(
                    field("name", action.getName()),
                    field("description", action.getDescription()),
                    field("parameters", action.getParameters())
            ));
            // @formatter:on
        }
        return json;
    }

    private static Object relationsToJson(final Set<RelationDescriptor> relations) {
        final List<Object> json = new ArrayList<Object>(relations.size());
        for (final RelationDescriptor relation : relations) {
            // @formatter:off
            json.add(object(
                    field("name", relation.getResourceName()),
                    field("description", relation.getDescription()),
                    field("multiplicity", relation.getMultiplicity()),
                    field("resource", String.valueOf(relation.getResource().getUrn()))
            ));
            // @formatter:on
        }
        return json;
    }

    public static RequestHandler newSingleApiDescriptorRequestHandler(final ApiDescriptor api) {
        return new AbstractRequestHandler() {

            @Override
            public void handleRead(final ServerContext context, final ReadRequest request,
                    final ResultHandler<Resource> handler) {
                if (request.getResourceNameObject().isEmpty()) {
                    handler.handleResult(new Resource(null, null, new JsonValue(apiToJson(api))));
                } else {
                    handler.handleError(new NotSupportedException());
                }
            }

        };
    }

    public static RequestHandler newMultiApiDescriptorRequestHandler(
            final Collection<ApiDescriptor> apis) {
        return new AbstractRequestHandler() {

            @Override
            public void handleRead(final ServerContext context, final ReadRequest request,
                    final ResultHandler<Resource> handler) {
                if (request.getResourceNameObject().isEmpty()) {
                    final List<Object> values = new ArrayList<Object>(apis.size());
                    for (final ApiDescriptor api : apis) {
                        values.add(apiToJson(api));
                    }
                    handler.handleResult(new Resource(null, null, new JsonValue(values)));
                } else {
                    handler.handleError(new NotSupportedException());
                }
            }

        };
    }

    public static RequestHandler newMultiApiDescriptorRequestHandler(final ApiDescriptor... apis) {
        return newMultiApiDescriptorRequestHandler(Arrays.asList(apis));
    }
}
