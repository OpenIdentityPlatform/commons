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
 * Copyright 2012 ForgeRock AS.
 */

package org.forgerock.json.resource.provider;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.Context;
import org.forgerock.json.resource.ContextAttribute;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResultHandler;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Request;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResultHandler;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.json.resource.exception.BadRequestException;
import org.forgerock.json.resource.exception.NotFoundException;
import org.forgerock.json.resource.exception.ResourceException;

/**
 * A resource provider which is capable of routing requests to a set of one or
 * more registered resource containers. Resource containers are associated with
 * a route which is specified as a URI template. Examples of valid URI templates
 * include:
 *
 * <pre>
 * /users
 * /users/{userId}/devices
 * </pre>
 *
 * Routers are thread safe and, in particular, support registration and
 * deregistration of resource containers while the router is handling requests.
 */
public final class Router implements ResourceProvider {

    /**
     * Contains the result of a URI template match.
     */
    private static final class Matcher {
        private final boolean matches;
        private final Map<String, String> variables;

        Matcher(final boolean matches, final Map<String, String> variables) {
            this.matches = matches;
            this.variables = variables;
        }

        boolean isMoreSpecificThan(final Matcher matcher) {
            if (matcher == null) {
                return true;
            } else {
                return variables.size() < matcher.variables.size();
            }
        }

        boolean matches() {
            return matches;
        }

        Map<String, String> variables() {
            return variables;
        }
    }

    /**
     * A mapping from a URI template to a resource provider implementation.
     */
    private static final class Route {
        private final Object provider;
        private final UriTemplate template;

        Route(final UriTemplate template, final ResourceCollection provider) {
            this.template = template;
            this.provider = provider;
        }

        Route(final UriTemplate template, final ResourceSingleton provider) {
            this.template = template;
            this.provider = provider;
        }

        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            } else if (obj instanceof Route) {
                return template.equals(((Route) obj).template);
            } else {
                return false;
            }
        }

        @Override
        public int hashCode() {
            return template.hashCode();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            final StringBuilder builder = new StringBuilder();
            builder.append('{');
            builder.append(template);
            builder.append(" -> ");
            builder.append(provider);
            builder.append('}');
            return builder.toString();
        }

        ResourceCollection getCollectionResourceProvider() {
            return (ResourceCollection) provider;
        }

        Matcher getMatcher(final String uri) {
            return template.matcher(uri);
        }

        ResourceSingleton getSingletonResourceProvider() {
            return (ResourceSingleton) provider;
        }

        RouteType getType() {
            return (provider instanceof ResourceCollection) ? RouteType.COLLECTION
                    : RouteType.SINGLETON;
        }

    }

    private static enum RouteType {
        COLLECTION, SINGLETON;
    }

    /**
     * A very simple URI template implementation based on RFC 6570.
     */
    private static final class UriTemplate {
        /**
         * A parsed template component: either a variable or a literal.
         */
        private static final class Element {
            private final boolean isVariable;
            private final String value;

            Element(final boolean isVariable, final String value) {
                this.isVariable = isVariable;
                this.value = value;
            }
        }

        private final List<Element> elements = new LinkedList<Element>();
        private final String normalizedTemplate;
        private final int numVariables;
        private final String template;

        UriTemplate(final String template) {
            final String t = normalizeUri(template);

            this.template = t;
            this.normalizedTemplate = t.replaceAll("\\{\\w\\}", "{x}");

            // Parse the template.
            int vcount = 0;
            boolean isInVariable = false;
            int elementStart = 0;
            int lastCloseBrace = -2;

            for (int i = 0; i < t.length(); i++) {
                if (isInVariable) {
                    final char c = template.charAt(i);
                    if (c == '}') {
                        if (elementStart == i) {
                            throw new IllegalArgumentException("URI template " + template
                                    + " contains zero-length template variable");
                        }
                        elements.add(new Element(true, template.substring(elementStart, i)));
                        isInVariable = false;
                        elementStart = i + 1;
                        lastCloseBrace = i;
                        vcount++;
                    } else if (!isValidVariableCharacter(c)) {
                        throw new IllegalArgumentException("URI template " + template
                                + " contains an illegal character " + c + " in a template variable");
                    } else {
                        // Continue counting characters in variable.
                    }
                } else if (template.charAt(i) == '{') {
                    if (lastCloseBrace == (i - 1)) {
                        throw new IllegalArgumentException("URI template " + template
                                + " contains consecutive template variables");
                    }
                    elements.add(new Element(false, template.substring(elementStart, i)));
                    isInVariable = true;
                    elementStart = i + 1;
                } else {
                    // Continue counting characters in literal.
                }
            }

            if (isInVariable) {
                // This shouldn't happen because the template always contains a
                // trailing '/' which is not a valid variable character.
                throw new IllegalArgumentException("URI template " + template
                        + " contains a trailing unclosed variable");
            } else {
                // Add trailing literal element.
                elements.add(new Element(false, template.substring(elementStart)));
            }

            this.numVariables = vcount;
        }

        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            } else if (obj instanceof UriTemplate) {
                return normalizedTemplate.equals(((UriTemplate) obj).normalizedTemplate);
            } else {
                return false;
            }
        }

        @Override
        public int hashCode() {
            return normalizedTemplate.hashCode();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return template;
        }

        Matcher matcher(final String uri) {
            // Fast-path for cases where the template does not contain any variables.
            if (numVariables == 0) {
                final String value = elements.iterator().next().value;
                if (value.equals(uri)) {
                    return new Matcher(true, Collections.<String, String> emptyMap());
                } else {
                    return NO_MATCH;
                }
            }

            // Template contains variables.
            final String nuri = normalizeUri(uri);
            int index = 0;
            final Map<String, String> variables = new LinkedHashMap<String, String>(numVariables);
            for (final Element e : elements) {
                if (index >= nuri.length()) {
                    // End of URI reached, but remaining template elements.
                    return NO_MATCH;
                }

                if (e.isVariable) {
                    // Parse variable up to next path separator.
                    final int end = nuri.indexOf('/', index);
                    final String field = nuri.substring(index, end);
                    variables.put(e.value, field);
                    index = end;
                } else {
                    // Parse literal content.
                    final String remainder = nuri.substring(index);
                    if (remainder.startsWith(e.value)) {
                        index += e.value.length();
                    } else {
                        return NO_MATCH;
                    }
                }
            }

            if (index < nuri.length()) {
                /*
                 * We've reached the end of the template yet there is still some
                 * of the URI remaining. If we were to support "starts with"
                 * semantics then we would indicate that the template matches.
                 */
                return NO_MATCH;
            }

            return new Matcher(true, Collections.unmodifiableMap(variables));
        }

        // As per RFC.
        private boolean isValidVariableCharacter(final char c) {
            return ((c >= 'a') && (c <= 'z')) || ((c >= 'A') && (c <= 'Z'))
                    || ((c >= '0') || (c <= '9')) || (c == '_');
        }

        // Ensure that URI contains a trailing '/' in order to make parsing a
        // matching simpler.
        private String normalizeUri(final String uri) {
            return uri.endsWith("/") ? uri : uri + "/";
        }
    }

    /**
     * The context attribute {@code org.forgerock.json.resource.provider.Router}
     * whose value is a {@code Map} containing the parsed URI template fields,
     * keyed on the URI template field name. This context attribute will be
     * added to the context once a request has been routed.
     */
    public static final ContextAttribute<Map<String, String>> URI_TEMPLATE_FIELDS =
            new ContextAttribute<Map<String, String>>(Router.class, Collections
                    .<String, String> emptyMap());

    /**
     * Cached Matcher used for failed match attempts.
     */
    private static final Matcher NO_MATCH = new Matcher(false, Collections
            .<String, String> emptyMap());

    // The registered set of routes.
    private final Set<Route> routes = new CopyOnWriteArraySet<Route>();

    /**
     * Creates a new empty router.
     */
    public Router() {
        // No implementation required.
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void action(final Context context, final ActionRequest request,
            final ResultHandler<JsonValue> handler) {
        try {
            final Route route = findMatchingRoute(context, request);
            switch (route.getType()) {
            case COLLECTION:
                if (request.getResourceId() != null) {
                    route.getCollectionResourceProvider().actionInstance(context, request, handler);
                } else {
                    route.getCollectionResourceProvider().actionCollection(context, request,
                            handler);
                }
                break;
            case SINGLETON:
                if (request.getResourceId() != null) {
                    // TODO: i18n
                    throw newBadRequestException(
                            "Redundant resource ID in action request for singleton resource %s",
                            request.getComponent());
                } else {
                    route.getSingletonResourceProvider().actionInstance(context, request, handler);
                }
                break;
            }
        } catch (final ResourceException e) {
            handler.handleError(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void create(final Context context, final CreateRequest request,
            final ResultHandler<Resource> handler) {
        try {
            final Route route = findMatchingRoute(context, request);
            switch (route.getType()) {
            case COLLECTION:
                // Resource ID is optional for create requests.
                route.getCollectionResourceProvider().createInstance(context, request, null);
                break;
            case SINGLETON:
                // TODO: i18n
                throw newBadRequestException("The singleton resource %s cannot be created", request
                        .getComponent());
            }
        } catch (final ResourceException e) {
            handler.handleError(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void delete(final Context context, final DeleteRequest request,
            final ResultHandler<Resource> handler) {
        try {
            final Route route = findMatchingRoute(context, request);
            switch (route.getType()) {
            case COLLECTION:
                if (request.getResourceId() != null) {
                    route.getCollectionResourceProvider().deleteInstance(context, request, null);
                } else {
                    // TODO: i18n
                    throw newBadRequestException(
                            "Missing resource ID in delete request for resource collection %s",
                            request.getComponent());
                }
                break;
            case SINGLETON:
                // TODO: i18n
                throw newBadRequestException("The singleton resource %s cannot be deleted", request
                        .getComponent());
            }
        } catch (final ResourceException e) {
            handler.handleError(e);
        }
    }

    /**
     * Deregisters the resource container associated with the specified URI
     * template, if present.
     *
     * @param uriTemplate
     *            The URI template of the resource container to be removed.
     * @return This router.
     */
    public Router deregisterResourceContainer(final String uriTemplate) {
        routes.remove(new Route(new UriTemplate(uriTemplate), (ResourceCollection) null));
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void patch(final Context context, final PatchRequest request,
            final ResultHandler<Resource> handler) {
        try {
            final Route route = findMatchingRoute(context, request);
            switch (route.getType()) {
            case COLLECTION:
                if (request.getResourceId() != null) {
                    route.getCollectionResourceProvider().patchInstance(context, request, null);
                } else {
                    // TODO: i18n
                    throw newBadRequestException(
                            "Missing resource ID in patch request for resource collection %s",
                            request.getComponent());
                }
                break;
            case SINGLETON:
                if (request.getResourceId() == null) {
                    route.getSingletonResourceProvider().patchInstance(context, request, null);
                } else {
                    // TODO: i18n
                    throw newBadRequestException(
                            "Redundant resource ID in patch request for singleton resource %s",
                            request.getComponent());
                }
            }
        } catch (final ResourceException e) {
            handler.handleError(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void query(final Context context, final QueryRequest request,
            final QueryResultHandler handler) {
        try {
            final Route route = findMatchingRoute(context, request);
            switch (route.getType()) {
            case COLLECTION:
                if (request.getResourceId() != null) {
                    // TODO: i18n
                    throw newBadRequestException(
                            "Redundant resource ID in query request for resource collection %s",
                            request.getComponent());
                } else {
                    route.getCollectionResourceProvider()
                            .queryCollection(context, request, handler);
                }
                break;
            case SINGLETON:
                // TODO: i18n
                throw newBadRequestException("The singleton resource %s cannot be queried", request
                        .getComponent());
            }
        } catch (final ResourceException e) {
            handler.handleError(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void read(final Context context, final ReadRequest request,
            final ResultHandler<Resource> handler) {
        try {
            final Route route = findMatchingRoute(context, request);
            switch (route.getType()) {
            case COLLECTION:
                if (request.getResourceId() != null) {
                    route.getCollectionResourceProvider().readInstance(context, request, null);
                } else {
                    // TODO: i18n
                    throw newBadRequestException(
                            "Missing resource ID in read request for resource collection %s",
                            request.getComponent());
                }
                break;
            case SINGLETON:
                if (request.getResourceId() == null) {
                    route.getSingletonResourceProvider().readInstance(context, request, null);
                } else {
                    // TODO: i18n
                    throw newBadRequestException(
                            "Redundant resource ID in read request for singleton resource %s",
                            request.getComponent());
                }
            }
        } catch (final ResourceException e) {
            handler.handleError(e);
        }
    }

    /**
     * Registers a resource collection with this router, replacing any existing
     * registered resource containers having the same URI template.
     *
     * @param uriTemplate
     *            The URI template associated with the resource collection.
     * @param container
     *            The resource collection container.
     * @return This router.
     */
    public Router registerResourceContainer(final String uriTemplate,
            final ResourceCollection container) {
        if (container == null) {
            throw new NullPointerException();
        }
        routes.add(new Route(new UriTemplate(uriTemplate), container));
        return this;
    }

    /**
     * Registers a singleton resource with this router, replacing any existing
     * registered resource containers having the same URI template.
     *
     * @param uriTemplate
     *            The URI template associated with the singleton resource.
     * @param container
     *            The singleton resource container.
     * @return This router.
     */
    public Router registerResourceContainer(final String uriTemplate,
            final ResourceSingleton container) {
        if (container == null) {
            throw new NullPointerException();
        }
        routes.add(new Route(new UriTemplate(uriTemplate), container));
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append('[');
        boolean isFirst = true;
        for (final Route route : routes) {
            if (isFirst) {
                isFirst = false;
            } else {
                builder.append(", ");
            }
            builder.append(route);
        }
        builder.append(']');
        return builder.toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void update(final Context context, final UpdateRequest request,
            final ResultHandler<Resource> handler) {
        try {
            final Route route = findMatchingRoute(context, request);
            switch (route.getType()) {
            case COLLECTION:
                if (request.getResourceId() != null) {
                    route.getCollectionResourceProvider().updateInstance(context, request, null);
                } else {
                    // TODO: i18n
                    throw newBadRequestException(
                            "Missing resource ID in update request for resource collection %s",
                            request.getComponent());
                }
                break;
            case SINGLETON:
                if (request.getResourceId() == null) {
                    route.getSingletonResourceProvider().updateInstance(context, request, null);
                } else {
                    // TODO: i18n
                    throw newBadRequestException(
                            "Redundant resource ID in update request for singleton resource %s",
                            request.getComponent());
                }
            }
        } catch (final ResourceException e) {
            handler.handleError(e);
        }
    }

    private Route findMatchingRoute(final Context context, final Request request)
            throws ResourceException {
        Matcher bestMatcher = null;
        Route bestRoute = null;

        for (final Route route : routes) {
            final Matcher matcher = route.getMatcher(request.getComponent());
            if (matcher.matches() && matcher.isMoreSpecificThan(bestMatcher)) {
                bestMatcher = matcher;
                bestRoute = route;
            }
        }
        if (bestMatcher != null) {
            URI_TEMPLATE_FIELDS.set(context, bestMatcher.variables());
            return bestRoute;
        }

        // TODO: i18n
        throw new NotFoundException(String.format("Resource %s not found", request.getComponent()));
    }

    private ResourceException newBadRequestException(final String fs, final Object... args) {
        final String msg = String.format(fs, args);
        return new BadRequestException(msg);
    }

}
