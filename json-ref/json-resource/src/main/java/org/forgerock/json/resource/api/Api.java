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

import static java.util.Collections.unmodifiableSet;
import static org.forgerock.json.fluent.JsonValue.field;
import static org.forgerock.json.fluent.JsonValue.json;
import static org.forgerock.json.fluent.JsonValue.object;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.forgerock.i18n.LocalizableMessage;
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
                relationsToJson(api.getRelations()),
                resourcesToJson(api.getResources()),
                profilesToJson(api.getProfiles())
        );
        // @formatter:on
    }

    private static Entry<String, Object> resourcesToJson(final Set<ResourceDescriptor> resources) {
        if (resources.isEmpty()) {
            return null;
        }
        final List<Object> json = new ArrayList<Object>(resources.size());
        for (final ResourceDescriptor resource : resources) {
            // @formatter:off
            json.add(object(
                    field("urn", String.valueOf(resource.getUrn())),
                    field("name", String.valueOf(resource.getUrn().getName())),
                    field("version", String.valueOf(resource.getUrn().getVersion())),
                    field("description", resource.getDescription()),
                    parentToJson(resource.getParentUrn()),
                    actionsToJson(resource.getActions()),
                    relationsToJson(resource.getRelations()),
                    profilesToJson(resource.getProfiles())
            ));
            // @formatter:on
        }
        return field("resources", json);
    }

    private static Entry<String, Object> parentToJson(final Urn parent) {
        return parent == null ? null : field("parent", String.valueOf(parent));
    }

    private static Entry<String, Object> actionsToJson(final Set<ActionDescriptor> actions) {
        if (actions.isEmpty()) {
            return null;
        }
        final List<Object> json = new ArrayList<Object>(actions.size());
        for (final ActionDescriptor action : actions) {
            // @formatter:off
            json.add(object(
                    field("name", action.getName()),
                    field("description", action.getDescription()),
                    parametersToJson(action.getParameters()),
                    profilesToJson(action.getProfiles())
            ));
            // @formatter:on
        }
        return field("actions", json);
    }

    private static Entry<String, Object> parametersToJson(final Set<ActionParameter> parameters) {
        if (parameters.isEmpty()) {
            return null;
        }
        final List<Object> json = new ArrayList<Object>(parameters.size());
        for (final ActionParameter parameter : parameters) {
            // @formatter:off
            json.add(object(
                    field("name", parameter.getName()),
                    field("description", parameter.getDescription())
            ));
            // @formatter:on
        }
        return field("parameters", json);
    }

    private static Entry<String, Object> relationsToJson(final Set<RelationDescriptor> relations) {
        if (relations.isEmpty()) {
            return null;
        }
        final List<Object> json = new ArrayList<Object>(relations.size());
        for (final RelationDescriptor relation : relations) {
            // @formatter:off
            json.add(object(
                    field("name", relation.getResourceName()),
                    field("description", relation.getDescription()),
                    field("multiplicity", relation.getMultiplicity()),
                    actionsToJson(relation.getActions()),
                    field("resource", String.valueOf(relation.getResource().getUrn())),
                    profilesToJson(relation.getProfiles())
            ));
            // @formatter:on
        }
        return field("relations", json);
    }

    private static Entry<String, Object> profilesToJson(final Set<Profile> profiles) {
        if (profiles.isEmpty()) {
            return null;
        }
        final List<Object> json = new ArrayList<Object>(profiles.size());
        for (final Profile profile : profiles) {
            // @formatter:off
            json.add(object(
                    field("urn", String.valueOf(profile.getUrn())),
                    field("name", String.valueOf(profile.getUrn().getName())),
                    field("version", String.valueOf(profile.getUrn().getVersion())),
                    field("content", profile.getContent().getObject())
            ));
            // @formatter:on
        }
        return field("profiles", json);
    }

    public static RequestHandler newSingleApiDescriptorRequestHandler(final ApiDescriptor api) {
        return new AbstractRequestHandler() {
            @Override
            public void handleRead(final ServerContext context, final ReadRequest request,
                    final ResultHandler<Resource> handler) {
                if (request.getResourceNameObject().isEmpty()) {
                    handler.handleResult(new Resource(null, null, json(apiToJson(api))));
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
                    handler.handleResult(new Resource(null, null, json(values)));
                } else {
                    handler.handleError(new NotSupportedException());
                }
            }
        };
    }

    public static RequestHandler newMultiApiDescriptorRequestHandler(final ApiDescriptor... apis) {
        return newMultiApiDescriptorRequestHandler(Arrays.asList(apis));
    }

    static <T> Set<T> unmodifiableCopyOf(final Collection<T> set) {
        return unmodifiableSet(new LinkedHashSet<T>(set));
    }

    static LocalizableMessage defaultToEmptyMessageIfNull(final LocalizableMessage description) {
        return description != null ? description : LocalizableMessage.EMPTY;
    }
}
