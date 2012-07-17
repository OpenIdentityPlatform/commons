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

import org.forgerock.json.resource.Context;
import org.forgerock.json.resource.ContextAttribute;
import org.forgerock.json.resource.Request;
import org.forgerock.json.resource.exception.NotFoundException;
import org.forgerock.json.resource.exception.ResourceException;

/**
 * A URI template based routing strategy which routes requests based on their
 * component name. Examples of valid URI templates include:
 *
 * <pre>
 * /users
 * /users/{userId}/devices
 * </pre>
 *
 * <b>NOTE:</b> for simplicity this implementation only supports a small sub-set
 * of the functionality described in RFC 6570.
 *
 * @see <a href="http://tools.ietf.org/html/rfc6570">RFC 6570 - URI Template
 *      </a>
 */
public final class UriTemplateRoutingStrategy implements RoutingStrategy {

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
        private final RoutingResult result;
        private final UriTemplate template;

        Route(final UriTemplate template, final CollectionResourceProvider provider) {
            this.template = template;
            this.result = new RoutingResult(provider);
        }

        Route(final UriTemplate template, final SingletonResourceProvider provider) {
            this.template = template;
            this.result = new RoutingResult(provider);
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
            builder.append(result);
            builder.append('}');
            return builder.toString();
        }

        Matcher getMatcher(final String uri) {
            return template.matcher(uri);
        }

        RoutingResult getResult() {
            return result;
        }
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
                    final char c = t.charAt(i);
                    if (c == '}') {
                        if (elementStart == i) {
                            throw new IllegalArgumentException("URI template " + t
                                    + " contains zero-length template variable");
                        }
                        elements.add(new Element(true, t.substring(elementStart, i)));
                        isInVariable = false;
                        elementStart = i + 1;
                        lastCloseBrace = i;
                        vcount++;
                    } else if (!isValidVariableCharacter(c)) {
                        throw new IllegalArgumentException("URI template " + t
                                + " contains an illegal character " + c + " in a template variable");
                    } else {
                        // Continue counting characters in variable.
                    }
                } else if (t.charAt(i) == '{') {
                    if (lastCloseBrace == (i - 1)) {
                        throw new IllegalArgumentException("URI template " + t
                                + " contains consecutive template variables");
                    }
                    elements.add(new Element(false, t.substring(elementStart, i)));
                    isInVariable = true;
                    elementStart = i + 1;
                } else {
                    // Continue counting characters in literal.
                }
            }

            if (isInVariable) {
                // This shouldn't happen because the template always contains a
                // trailing '/' which is not a valid variable character.
                throw new IllegalArgumentException("URI template " + t
                        + " contains a trailing unclosed variable");
            } else {
                // Add trailing literal element.
                elements.add(new Element(false, t.substring(elementStart)));
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
            final String nuri = normalizeUri(uri);

            // Fast-path for cases where the template does not contain any variables.
            if (numVariables == 0) {
                final String value = elements.iterator().next().value;
                if (value.equals(nuri)) {
                    return new Matcher(true, Collections.<String, String> emptyMap());
                } else {
                    return NO_MATCH;
                }
            }

            // Template contains variables.
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
                    || ((c >= '0') && (c <= '9')) || (c == '_');
        }

        // Ensure that URI contains a trailing '/' in order to make parsing a
        // matching simpler.
        private String normalizeUri(final String uri) {
            return uri.endsWith("/") ? uri : uri + "/";
        }
    }

    /**
     * The context attribute {@code uri-template-variables} whose value is a
     * {@code Map} containing the parsed URI template variables, keyed on the
     * URI template variable name. This context attribute will be added to the
     * context once a request has been routed.
     */
    public static final ContextAttribute<Map<String, String>> URI_TEMPLATE_VARIABLES =
            new ContextAttribute<Map<String, String>>("uri-template-variables", Collections
                    .<String, String> emptyMap());

    /**
     * Cached Matcher used for failed match attempts.
     */
    private static final Matcher NO_MATCH = new Matcher(false, Collections
            .<String, String> emptyMap());

    // The registered set of routes.
    private final Set<Route> routes = new CopyOnWriteArraySet<Route>();

    /**
     * Creates a new URI template routing strategy with no routes defined.
     */
    public UriTemplateRoutingStrategy() {
        // No implementation required.
    }

    /**
     * Deregisters the resource provider associated with the specified URI
     * template, if present.
     *
     * @param uriTemplate
     *            The URI template of the resource provider to be removed.
     * @return This router.
     */
    public UriTemplateRoutingStrategy deregister(final String uriTemplate) {
        routes.remove(new Route(new UriTemplate(uriTemplate), (CollectionResourceProvider) null));
        return this;
    }

    /**
     * Registers a collection resource provider with this router, replacing any
     * existing registered resource providers having the same URI template.
     *
     * @param uriTemplate
     *            The URI template associated with the collection.
     * @param provider
     *            The collection resource provider.
     * @return This router.
     */
    public UriTemplateRoutingStrategy register(final String uriTemplate,
            final CollectionResourceProvider provider) {
        if (provider == null) {
            throw new NullPointerException();
        }
        replaceRoute(new Route(new UriTemplate(uriTemplate), provider));
        return this;
    }

    /**
     * Registers a singleton resource provider with this router, replacing any
     * existing registered resource provider having the same URI template.
     *
     * @param uriTemplate
     *            The URI template associated with the singleton.
     * @param provider
     *            The singleton resource provider.
     * @return This router.
     */
    public UriTemplateRoutingStrategy register(final String uriTemplate,
            final SingletonResourceProvider provider) {
        if (provider == null) {
            throw new NullPointerException();
        }
        replaceRoute(new Route(new UriTemplate(uriTemplate), provider));
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RoutingResult routeRequest(final Context context, final Request request)
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
            URI_TEMPLATE_VARIABLES.set(context, bestMatcher.variables());
            return bestRoute.getResult();
        }

        // TODO: i18n
        throw new NotFoundException(String.format("Resource %s not found", request.getComponent()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("UriTemplate(");
        boolean isFirst = true;
        for (final Route route : routes) {
            if (isFirst) {
                isFirst = false;
            } else {
                builder.append(", ");
            }
            builder.append(route);
        }
        builder.append(')');
        return builder.toString();
    }

    private void replaceRoute(final Route route) {
        // Ensure that existing route is removed.
        routes.remove(route);
        routes.add(route);
    }

}
