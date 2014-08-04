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

import static org.forgerock.json.resource.ResourceName.urlDecode;
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
 * An opaque handle for a route which has been registered in a {@link UriRouter
 * router}. A reference to a route should be maintained if there is a chance
 * that the route will need to be removed from the router at a later time.
 *
 * @see UriRouter
 */
final class UriRoute implements Route {
    static final class RouteMatcher {
        private final RouterContext context;
        private final RequestHandler handler;
        private final String match;
        private final String remaining;
        private final UriTemplate template;

        // Constructor for default route.
        RouteMatcher(final ServerContext context, final RequestHandler handler) {
            this.template = null;
            this.match = null;
            this.remaining = null;
            this.context = new RouterContext(context, "", Collections.<String, String> emptyMap());
            this.handler = handler;
        }

        // Constructor for matching template.
        RouteMatcher(final UriTemplate template, final String match, final String remaining,
                final RouterContext context) {
            this.template = template;
            this.match = match;
            this.remaining = remaining;
            this.context = context;
            this.handler = template.getRoute().getRequestHandler();
        }

        String getRemaining() {
            return remaining;
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

        boolean wasRouted() {
            return remaining != null;
        }
    }

    private final class UriTemplate {
        private final RoutingMode mode;
        private final Pattern regex;
        private final String uriTemplate;
        private final List<String> variables = new LinkedList<String>();

        private UriTemplate(final RoutingMode mode, final String uriTemplate) {
            final String t = removeTrailingSlash(removeLeadingSlash(uriTemplate));
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
                throw new IllegalArgumentException("URI template " + t
                        + " contains a trailing unclosed variable");
            }

            // Escape and add remaining literal substring.
            builder.append(Pattern.quote(t.substring(elementStart)));
            builder.append(')');

            if (mode == STARTS_WITH) {
                // Add wild-card match for remaining unmatched path.
                if (uriTemplate.isEmpty()) {
                    /*
                     * Special case for empty template: the next path element is
                     * not preceded by a slash. The redundant parentheses are
                     * required in order to have consistent group numbering with
                     * the non-empty case.
                     */
                    builder.append("((.*))?");
                } else {
                    builder.append("(/(.*))?");
                }
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

        private UriRoute getRoute() {
            return UriRoute.this;
        }

        private RouteMatcher getRouteMatcher(final UriRoute route, final ServerContext context,
                final Request request) {
            final String resourceName = request.getResourceName();
            final Matcher matcher = regex.matcher(resourceName);
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
                variableMap = Collections.singletonMap(variables.get(0), urlDecode(matcher.group(2)));
                break;
            default:
                variableMap = new LinkedHashMap<String, String>(variables.size());
                for (int i = 0; i < variables.size(); i++) {
                    // Group 0 matches entire URL, group 1 matches entire template.
                    variableMap.put(variables.get(i), urlDecode(matcher.group(i + 2)));
                }
                break;
            }
            final String remaining = removeLeadingSlash(resourceName.substring(matcher.end(1)));
            final String matched = matcher.group(1);
            return new RouteMatcher(this, matcher.group(1), remaining, new RouterContext(context,
                    matched, variableMap));
        }

        private String removeLeadingSlash(final String resourceName) {
            if (resourceName.startsWith("/")) {
                return resourceName.substring(1);
            }
            return resourceName;
        }

        private String removeTrailingSlash(final String resourceName) {
            if (resourceName.endsWith("/")) {
                return resourceName.substring(0, resourceName.length() - 1);
            }
            return resourceName;
        }

        // As per RFC.
        private boolean isValidVariableCharacter(final char c) {
            return ((c >= 'a') && (c <= 'z')) || ((c >= 'A') && (c <= 'Z'))
                    || ((c >= '0') && (c <= '9')) || (c == '_');
        }
    }

    private final RequestHandler handler;
    private final UriTemplate template;

    UriRoute(final RoutingMode mode, final String uriTemplate, final RequestHandler handler) {
        this.template = new UriTemplate(mode, uriTemplate);
        this.handler = handler;
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
}
