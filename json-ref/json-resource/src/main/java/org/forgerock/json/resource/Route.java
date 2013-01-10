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
 * Copyright 2012-2013 ForgeRock AS.
 */

package org.forgerock.json.resource;

import static org.forgerock.json.resource.RoutingMode.EQUALS;
import static org.forgerock.json.resource.RoutingMode.STARTS_WITH;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * An opaque handle for a route which has been registered in a router. A
 * reference to a route should be maintained if there is a chance that the route
 * will need to be removed from the router at a later time.
 */
public final class Route {
    static final class RouteMatcher {
        private final RouterContext context;
        private final RequestHandler handler;
        private final String match;
        private final UriTemplate template;

        RouteMatcher(final ServerContext context, final RequestHandler handler) {
            this.template = null;
            this.match = null;
            this.context = new RouterContext(context, Collections.<String, String> emptyMap());
            this.handler = handler;
        }

        RouteMatcher(final UriTemplate template, final String match, final RouterContext context) {
            this.template = template;
            this.match = match;
            this.context = context;
            this.handler = template.getRoute().getRequestHandler();
        }

        RequestHandler getRequestHandler() {
            return handler;
        }

        ServerContext getServerContext() {
            return context;
        }

        boolean isBetterMatchThan(final RouteMatcher matcher) {
            if (matcher == null) {
                return true;
            } else if (!match.equals(matcher.match)) {
                // One template matched a greater proportion of the resource
                // name than the other. Use the template which matched the most.
                return match.length() > matcher.match.length();
            } else if (template.mode != matcher.template.mode) {
                // Prefer equality match over startsWith match.
                return template.mode == EQUALS;
            } else {
                // Prefer a match with less variables.
                return context.getUriTemplateVariables().size() < matcher.context
                        .getUriTemplateVariables().size();
            }
        }
    }

    private final class UriTemplate {
        private final RoutingMode mode;
        private final Pattern regex;
        private final String uriTemplate;
        private final List<String> variables = new LinkedList<String>();

        private UriTemplate(final RoutingMode mode, final String uriTemplate) {
            final String t = normalizeUri(uriTemplate);
            final StringBuilder builder = new StringBuilder(t.length() + 8);

            // Parse the template.
            boolean isInVariable = false;
            int elementStart = 0;
            builder.append('('); // Group 1 does not include trailing portion for STARTS_WITH.
            for (int i = 0; i < t.length(); i++) {
                final char c = t.charAt(i);
                if (isInVariable) {
                    if (c == '}') {
                        if (elementStart == i) {
                            throw new IllegalArgumentException("URI template " + t
                                    + " contains zero-length template variable");
                        }
                        variables.add(t.substring(elementStart, i));
                        builder.append("([^/]+)");
                        isInVariable = false;
                        elementStart = i + 1;
                    } else if (!isValidVariableCharacter(c)) {
                        throw new IllegalArgumentException("URI template " + t
                                + " contains an illegal character " + c + " in a template variable");
                    } else {
                        // Continue counting characters in variable.
                    }
                } else if (c == '{') {
                    // Escape and add literal substring.
                    builder.append(Pattern.quote(t.substring(elementStart, i)));

                    isInVariable = true;
                    elementStart = i + 1;
                }
            }

            if (isInVariable) {
                // This shouldn't happen because the template always contains a
                // trailing '/' which is not a valid variable character.
                throw new IllegalArgumentException("URI template " + t
                        + " contains a trailing unclosed variable");
            }

            // Escape and add remaining literal substring.
            builder.append(Pattern.quote(t.substring(elementStart)));
            builder.append(')');

            if (mode == STARTS_WITH) {
                // Add wild-card match for remaining unmatched path (not included in group 1).
                builder.append(".*");
            }

            this.uriTemplate = uriTemplate;
            this.mode = mode;
            this.regex = Pattern.compile(builder.toString());
        }

        @Override
        public String toString() {
            final StringBuilder builder = new StringBuilder();
            if (mode == EQUALS) {
                builder.append("equals(");
            } else {
                builder.append("startsWith(");
            }
            builder.append(uriTemplate);
            builder.append(')');
            return builder.toString();
        }

        private Route getRoute() {
            return Route.this;
        }

        private RouteMatcher getRouteMatcher(final Route route, final ServerContext context,
                final Request request) {
            final String uri = normalizeUri(request.getResourceName());
            final Matcher matcher = regex.matcher(uri);
            if (!matcher.matches()) {
                return null;
            }
            final Map<String, String> variableMap;
            switch (variables.size()) {
            case 0:
                variableMap = Collections.emptyMap();
                break;
            case 1:
                // Group 0 matches entire URL, group 1 matches entire template.
                variableMap = Collections.singletonMap(variables.get(0), matcher.group(2));
                break;
            default:
                variableMap = new LinkedHashMap<String, String>(variables.size());
                for (int i = 0; i < variables.size(); i++) {
                    // Group 0 matches entire URL, group 1 matches entire template.
                    variableMap.put(variables.get(i), matcher.group(i + 2));
                }
                break;
            }
            return new RouteMatcher(this, matcher.group(), new RouterContext(context, variableMap));
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

    private final RequestHandler handler;
    private final Route subRoute;
    private final UriTemplate template;

    Route(final RoutingMode mode, final String uriTemplate, final RequestHandler handler,
            final Route subRoute) {
        this.template = new UriTemplate(mode, uriTemplate);
        this.handler = handler;
        this.subRoute = subRoute;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append('{');
        builder.append(template);
        builder.append(" -> ");
        builder.append(handler);
        builder.append('}');
        return builder.toString();
    }

    RequestHandler getRequestHandler() {
        return handler;
    }

    RouteMatcher getRouteMatcher(final ServerContext context, final Request request) {
        return template.getRouteMatcher(this, context, request);
    }

    Route getSubRoute() {
        return subRoute;
    }
}
