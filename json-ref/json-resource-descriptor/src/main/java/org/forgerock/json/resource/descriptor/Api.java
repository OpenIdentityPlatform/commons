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
 * Copyright 2013-2015 ForgeRock AS.
 */

package org.forgerock.json.resource.descriptor;

import static java.util.Collections.unmodifiableSet;
import static org.forgerock.json.fluent.JsonValue.*;
import static org.forgerock.json.resource.Requests.*;
import static org.forgerock.json.resource.ResourceException.newNotSupportedException;
import static org.forgerock.util.promise.Promises.newExceptionPromise;
import static org.forgerock.util.promise.Promises.newResultPromise;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.forgerock.http.ResourcePath;
import org.forgerock.http.ServerContext;
import org.forgerock.i18n.LocalizableMessage;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.AbstractRequestHandler;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResult;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Request;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.json.resource.descriptor.RelationDescriptor.Multiplicity;
import org.forgerock.util.AsyncFunction;
import org.forgerock.util.promise.Promise;

@SuppressWarnings("javadoc")
public final class Api {
    static final ObjectMapper JSON_MAPPER = new ObjectMapper();

    public static RequestHandler newApiDescriptorRequestHandler(final ApiDescriptor api) {
        return new AbstractRequestHandler() {
            @Override
            public Promise<Resource, ResourceException> handleRead(final ServerContext context,
                    final ReadRequest request) {
                if (request.getResourcePathObject().isEmpty()) {
                    return newResultPromise(new Resource(null, null, json(apiToJson(api))));
                } else {
                    return newExceptionPromise(newNotSupportedException());
                }
            }
        };
    }

    public static RequestHandler newApiDescriptorRequestHandler(final Collection<ApiDescriptor> apis) {
        return new AbstractRequestHandler() {
            @Override
            public Promise<Resource, ResourceException> handleRead(final ServerContext context,
                    final ReadRequest request) {
                if (request.getResourcePathObject().isEmpty()) {
                    final List<Object> values = new ArrayList<>(apis.size());
                    for (final ApiDescriptor api : apis) {
                        values.add(apiToJson(api));
                    }
                    return newResultPromise(new Resource(null, null, json(values)));
                } else {
                    return newExceptionPromise(newNotSupportedException());
                }
            }
        };
    }

    public static RequestHandler newApiDispatcher(final ApiDescriptor api,
            final ResolverFactory factory) {
        return new RequestHandler() {

            @Override
            public Promise<JsonValue, ResourceException> handleAction(final ServerContext context,
                    final ActionRequest request) {
                final ActionRequest mutableCopy = copyOfActionRequest(request);
                final Resolver resolver = factory.createResolver(context, request);
                return resolveAndInvoke(api.getRelations(), mutableCopy, resolver)
                        .thenAsync(new AsyncFunction<RequestHandler, JsonValue, ResourceException>() {
                            @Override
                            public Promise<JsonValue, ResourceException> apply(RequestHandler resolvedRequestHandler) {
                                return resolvedRequestHandler.handleAction(context, mutableCopy);
                            }
                        });
            }

            @Override
            public Promise<Resource, ResourceException> handleCreate(final ServerContext context,
                    final CreateRequest request) {
                final CreateRequest mutableCopy = copyOfCreateRequest(request);
                final Resolver resolver = factory.createResolver(context, request);
                return resolveAndInvoke(api.getRelations(), mutableCopy, resolver)
                        .thenAsync(new AsyncFunction<RequestHandler, Resource, ResourceException>() {
                            @Override
                            public Promise<Resource, ResourceException> apply(RequestHandler resolvedRequestHandler) {
                                return resolvedRequestHandler.handleCreate(context, mutableCopy);
                            }
                        });
            }

            @Override
            public Promise<Resource, ResourceException> handleDelete(final ServerContext context,
                    final DeleteRequest request) {
                final DeleteRequest mutableCopy = copyOfDeleteRequest(request);
                final Resolver resolver = factory.createResolver(context, request);
                return resolveAndInvoke(api.getRelations(), mutableCopy, resolver)
                        .thenAsync(new AsyncFunction<RequestHandler, Resource, ResourceException>() {
                            @Override
                            public Promise<Resource, ResourceException> apply(RequestHandler resolvedRequestHandler) {
                                return resolvedRequestHandler.handleDelete(context, mutableCopy);
                            }
                        });
            }

            @Override
            public Promise<Resource, ResourceException> handlePatch(final ServerContext context,
                    final PatchRequest request) {
                final PatchRequest mutableCopy = copyOfPatchRequest(request);
                final Resolver resolver = factory.createResolver(context, request);
                return resolveAndInvoke(api.getRelations(), mutableCopy, resolver)
                        .thenAsync(new AsyncFunction<RequestHandler, Resource, ResourceException>() {
                            @Override
                            public Promise<Resource, ResourceException> apply(RequestHandler resolvedRequestHandler) {
                                return resolvedRequestHandler.handlePatch(context, mutableCopy);
                            }
                        });
            }

            @Override
            public Promise<QueryResult, ResourceException> handleQuery(final ServerContext context,
                    final QueryRequest request, final QueryResourceHandler handler) {
                final QueryRequest mutableCopy = copyOfQueryRequest(request);
                final Resolver resolver = factory.createResolver(context, request);
                return resolveAndInvoke(api.getRelations(), mutableCopy, resolver)
                        .thenAsync(new AsyncFunction<RequestHandler, QueryResult, ResourceException>() {
                            @Override
                            public Promise<QueryResult, ResourceException> apply(RequestHandler resolvedRequestHandler) {
                                return resolvedRequestHandler.handleQuery(context, mutableCopy, handler);
                            }
                        });
            }

            @Override
            public Promise<Resource, ResourceException> handleRead(final ServerContext context,
                    final ReadRequest request) {
                final ReadRequest mutableCopy = copyOfReadRequest(request);
                final Resolver resolver = factory.createResolver(context, request);
                return resolveAndInvoke(api.getRelations(), mutableCopy, resolver)
                        .thenAsync(new AsyncFunction<RequestHandler, Resource, ResourceException>() {
                            @Override
                            public Promise<Resource, ResourceException> apply(RequestHandler resolvedRequestHandler) {
                                return resolvedRequestHandler.handleRead(context, mutableCopy);
                            }
                        });
            }

            @Override
            public Promise<Resource, ResourceException> handleUpdate(final ServerContext context,
                    final UpdateRequest request) {
                final UpdateRequest mutableCopy = copyOfUpdateRequest(request);
                final Resolver resolver = factory.createResolver(context, request);
                return resolveAndInvoke(api.getRelations(), mutableCopy, resolver)
                        .thenAsync(new AsyncFunction<RequestHandler, Resource, ResourceException>() {
                            @Override
                            public Promise<Resource, ResourceException> apply(RequestHandler resolvedRequestHandler) {
                                return resolvedRequestHandler.handleUpdate(context, mutableCopy);
                            }
                        });
            }

            private boolean isBetterMatch(final RelationDescriptor oldMatch,
                    final RelationDescriptor newMatch) {
                return oldMatch == null
                        || oldMatch.getResourcePathObject().size() < newMatch
                        .getResourcePathObject().size();
            }

            private boolean isChildRequest(final ResourcePath relationName,
                    final ResourcePath target) {
                return target.size() == relationName.size() + 1;
            }

            private boolean isOneToMany(final RelationDescriptor relation) {
                return relation.getMultiplicity() == Multiplicity.ONE_TO_MANY;
            }

            private Promise<RequestHandler, ResourceException> resolveAndInvoke(
                    final Collection<RelationDescriptor> relations, final Request mutableRequest,
                    final Resolver resolver) {
                return resolveAndInvoke0(relations, mutableRequest, resolver)
                        .thenAlways(new Runnable() {
                            @Override
                            public void run() {
                                resolver.close();
                            }
                        });
            }

            private Promise<RequestHandler, ResourceException> resolveAndInvoke0(
                    final Collection<RelationDescriptor> relations, final Request mutableRequest,
                    final Resolver resolver) {
                // @formatter:off
                /*
                 * We need to find the best match so first try all
                 * relations to see if there is an exact match, then try
                 * all one-to-many relations to see if there is a child
                 * match, then try all relations to see if there is a
                 * starts with match for sub-resources. In other words:
                 *
                 * singleton
                 * collection
                 * collection/{id}
                 * collection/{id}/*
                 * singleton/*
                 */
                // @formatter:on
                final ResourcePath name = mutableRequest.getResourcePathObject();
                RelationDescriptor exactMatch = null;
                RelationDescriptor childMatch = null;
                RelationDescriptor subMatch = null;
                for (final RelationDescriptor relation : relations) {
                    final ResourcePath relationName = relation.getResourcePathObject();
                    if (name.equals(relationName)) {
                        /*
                         * Got an exact match - this wins outright so no point
                         * in continuing.
                         */
                        exactMatch = relation;
                        break;
                    } else if (name.startsWith(relationName)) {
                        if (isOneToMany(relation) && isChildRequest(relationName, name)) {
                            // Child match.
                            childMatch = relation;
                        } else if (isBetterMatch(subMatch, relation)) {
                            /*
                             * Sub-resource match: the new relation is more
                             * specific than the old one.
                             */
                            subMatch = relation;
                        }
                    }
                }
                if (exactMatch != null || childMatch != null) {
                    try {
                        RequestHandler resolvedRequestHandler;
                        if (exactMatch != null) {
                            resolvedRequestHandler = resolver.getRequestHandler(exactMatch);
                            mutableRequest.setResourcePath(ResourcePath.empty());
                        } else {
                            resolvedRequestHandler = resolver.getRequestHandler(childMatch);
                            mutableRequest.setResourcePath(name.tail(name.size() - 1));
                        }
                        return newResultPromise(resolvedRequestHandler);
                    } catch (final ResourceException e) {
                        return newExceptionPromise(e);
                    }
                } else if (subMatch != null) {
                    final String childId;
                    final int relationNameSize = subMatch.getResourcePathObject().size();
                    if (isOneToMany(subMatch)) {
                        // Strip off collection name and resource ID.
                        mutableRequest.setResourcePath(name.tail(relationNameSize + 1));
                        childId = name.get(relationNameSize);
                    } else {
                        // Strip off resource name.
                        mutableRequest.setResourcePath(name.tail(relationNameSize));
                        childId = null;
                    }
                    return resolver.getRelationsForResource(subMatch, childId)
                            .thenAsync(new AsyncFunction<Collection<RelationDescriptor>, RequestHandler, ResourceException>() {
                                @Override
                                public Promise<RequestHandler, ResourceException> apply(Collection<RelationDescriptor> result) {
                                    return resolveAndInvoke(result, mutableRequest, resolver);
                                }
                            });
                } else {
                    ResourceException e = new NotFoundException(String.format("Resource '%s' not found", name));
                    return newExceptionPromise(e);
                }
            }
        };
    }

    static LocalizableMessage defaultToEmptyMessageIfNull(final LocalizableMessage description) {
        return description != null ? description : LocalizableMessage.EMPTY;
    }

    static <T> Set<T> unmodifiableCopyOf(final Collection<T> set) {
        return unmodifiableSet(new LinkedHashSet<>(set));
    }

    private static Entry<String, Object> actionsToJson(final Set<ActionDescriptor> actions) {
        if (actions.isEmpty()) {
            return null;
        }
        final List<Object> json = new ArrayList<>(actions.size());
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

    private static Entry<String, Object> parametersToJson(final Set<ActionParameter> parameters) {
        if (parameters.isEmpty()) {
            return null;
        }
        final List<Object> json = new ArrayList<>(parameters.size());
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

    private static Entry<String, Object> parentToJson(final Urn parent) {
        return parent == null ? null : field("parent", String.valueOf(parent));
    }

    private static Entry<String, Object> profilesToJson(final Set<Profile> profiles) {
        if (profiles.isEmpty()) {
            return null;
        }
        final List<Object> json = new ArrayList<>(profiles.size());
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

    private static Entry<String, Object> relationsToJson(final Set<RelationDescriptor> relations) {
        if (relations.isEmpty()) {
            return null;
        }
        final List<Object> json = new ArrayList<>(relations.size());
        for (final RelationDescriptor relation : relations) {
            // @formatter:off
            json.add(object(
                    field("name", relation.getResourcePath()),
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

    private static Entry<String, Object> resourcesToJson(final Set<ResourceDescriptor> resources) {
        if (resources.isEmpty()) {
            return null;
        }
        final List<Object> json = new ArrayList<>(resources.size());
        for (final ResourceDescriptor resource : resources) {
            // @formatter:off
            json.add(object(
                    field("urn", String.valueOf(resource.getUrn())),
                    field("name", String.valueOf(resource.getUrn().getName())),
                    field("version", String.valueOf(resource.getUrn().getVersion())),
                    field("description", resource.getDescription()),
                    field("schema", JSON_MAPPER.convertValue(resource.getSchema(), Map.class)),
                    parentToJson(resource.getParentUrn()),
                    actionsToJson(resource.getActions()),
                    relationsToJson(resource.getRelations()),
                    profilesToJson(resource.getProfiles())
            ));
            // @formatter:on
        }
        return field("resources", json);
    }

    private Api() {
        // Nothing to do.
    }
}
