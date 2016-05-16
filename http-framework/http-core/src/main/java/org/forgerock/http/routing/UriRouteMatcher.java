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
 * Copyright 2012-2016 ForgeRock AS.
 */

package org.forgerock.http.routing;

import static org.forgerock.http.routing.RoutingMode.*;
import static org.forgerock.http.util.Paths.*;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.forgerock.http.ApiProducer;
import org.forgerock.services.context.Context;
import org.forgerock.services.routing.IncomparableRouteMatchException;
import org.forgerock.services.routing.RouteMatch;
import org.forgerock.services.routing.RouteMatcher;

/**
 * A {@link RouteMatcher} which routes requests using URI template matching
 * against the request URI. Examples of valid URI templates include:
 *
 * <pre>
 * users
 * users/{userId}
 * users/{userId}/devices
 * users/{userId}/devices/{deviceId}
 * </pre>
 *
 * Routes may be added and removed from a router as follows:
 *
 * <pre>
 * Handler users = ...;
 * Router router = new Router();
 * RouteMatcher routeOne = new UriRouteMatcher(EQUALS, &quot;users&quot;);
 * RouteMatcher routeTwo = new UriRouteMatcher(EQUALS, &quot;users/{userId}&quot;);
 * router.addRoute(routeOne, users);
 * router.addRoute(routeTwo, users);
 *
 * // Deregister a route.
 * router.removeRoute(routeOne, routeTwo);
 * </pre>
 *
 * A request handler receiving a routed request may access the associated
 * route's URI template variables via
 * {@link UriRouterContext#getUriTemplateVariables()}. For example, a handler
 * processing requests for the route users/{userId} may obtain the value of
 * {@code userId} as follows:
 *
 * <pre>
 * String userId = context.asContext(UriRouterContext.class).getUriTemplateVariables().get(&quot;userId&quot;);
 * </pre>
 *
 * <b>NOTE:</b> for simplicity this implementation only supports a small
 * sub-set of the functionality described in RFC 6570.
 */
class UriRouteMatcher extends RouteMatcher<List<String>> {

    private final RoutingMode mode;
    private final Pattern regex;
    private final String uriTemplate;
    private final List<String> variables = new LinkedList<>();

    /**
     * Creates a new URI route matcher which will match the given uri template.
     *
     * @param mode Indicates how the URI template should be matched against
     *            request URIs.
     * @param uriTemplate The URI template which request URIs must match.
     */
    UriRouteMatcher(RoutingMode mode, String uriTemplate) {
        this.uriTemplate = uriTemplate;
        this.mode = mode;
        this.regex = UriTemplateParser.createRegex(mode, uriTemplate, variables);
    }

    @Override
    public final RouteMatch evaluate(final Context context, final List<String> pathElements) {
        String uri = joinPath(pathElements);
        Matcher matcher = regex.matcher(uri);
        if (!matcher.matches()) {
            return null;
        }
        Map<String, String> variableMap;
        switch (variables.size()) {
        case 0:
            variableMap = Collections.emptyMap();
            break;
        case 1:
            // Group 0 matches entire URL, group 1 matches entire template.
            variableMap = Collections.singletonMap(variables.get(0), urlDecode(matcher.group(2)));
            break;
        default:
            variableMap = new LinkedHashMap<>(variables.size());
            for (int i = 0; i < variables.size(); i++) {
                // Group 0 matches entire URL, group 1 matches entire template.
                variableMap.put(variables.get(i), urlDecode(matcher.group(i + 2)));
            }
            break;
        }
        String remaining = UriTemplateParser.removeLeadingSlash(uri.substring(matcher.end(1)));
        return new UriRouteMatch(matcher.group(1), remaining, variableMap, mode);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        if (mode == EQUALS) {
            builder.append("equals(");
        } else {
            builder.append("startsWith(");
        }
        builder.append(uriTemplate);
        builder.append(')');
        return builder.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof UriRouteMatcher)) {
            return false;
        }

        UriRouteMatcher that = (UriRouteMatcher) o;

        if (mode != that.mode) {
            return false;
        }
        if (!regex.toString().equals(that.regex.toString())) {
            return false;
        }
        if (!uriTemplate.equals(that.uriTemplate)) {
            return false;
        }
        return variables.equals(that.variables);

    }

    @Override
    public String idFragment() {
        return "/" + uriTemplate;
    }

    @Override
    public <D> D transformApi(D descriptor, ApiProducer<D> producer) {
        return descriptor != null ? producer.withPath(descriptor, uriTemplate) : null;
    }

    @Override
    public int hashCode() {
        int result = mode.hashCode();
        result = 31 * result + regex.toString().hashCode();
        result = 31 * result + uriTemplate.hashCode();
        result = 31 * result + variables.hashCode();
        return result;
    }

    /**
     * Parses routing mode and uri templates into regular expression for
     * matching incoming request URIs.
     */
    private static final class UriTemplateParser {

        /**
         * Creates a regular expression from the given {@literal mode} and {@literal uriTemplate}.
         *
         * @param mode Indicates how the URI template should be matched against
         *             request URIs.
         * @param uriTemplate The URI template which request URIs must match.
         * @param variables An empty {@code List} of {@code String}s that uri
         *                  template variables will be added to.
         * @return A regular expression.
         */
        static Pattern createRegex(RoutingMode mode, String uriTemplate, List<String> variables) {
            String t = removeTrailingSlash(removeLeadingSlash(uriTemplate));
            StringBuilder builder = new StringBuilder(t.length() + 8);

            // Parse the template.
            boolean isInVariable = false;
            int elementStart = 0;
            builder.append('('); // Group 1 does not include trailing portion for STARTS_WITH.
            for (int i = 0; i < t.length(); i++) {
                char c = t.charAt(i);
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

            return Pattern.compile(builder.toString());
        }

        /**
         * Removes the leading slash, if present, from the resource name.
         *
         * @param resourceName The resource name.
         * @return The resource name without a leading slash.
         */
        static String removeLeadingSlash(String resourceName) {
            if (resourceName.startsWith("/")) {
                return resourceName.substring(1);
            }
            return resourceName;
        }

        private static String removeTrailingSlash(String resourceName) {
            if (resourceName.endsWith("/")) {
                return resourceName.substring(0, resourceName.length() - 1);
            }
            return resourceName;
        }

        // As per RFC.
        private static boolean isValidVariableCharacter(char c) {
            return ((c >= 'a') && (c <= 'z')) || ((c >= 'A') && (c <= 'Z'))
                    || ((c >= '0') && (c <= '9')) || (c == '_');
        }
    }

    /**
     * Contains the result of routing to a URI based route.
     */
    private static final class UriRouteMatch implements RouteMatch {

        private final String matched;
        private final String remaining;
        private final Map<String, String> variableMap;
        private final RoutingMode mode;

        private UriRouteMatch(String matched, String remaining, Map<String, String> variableMap, RoutingMode mode) {
            this.matched = matched;
            this.remaining = remaining;
            this.variableMap = variableMap;
            this.mode = mode;
        }

        @Override
        public boolean isBetterMatchThan(RouteMatch routeMatch) throws IncomparableRouteMatchException {
            if (routeMatch == null) {
                return true;
            } else if (!(routeMatch instanceof UriRouteMatch)) {
                throw new IncomparableRouteMatchException(this, routeMatch);
            }

            UriRouteMatch result = (UriRouteMatch) routeMatch;
            if (!matched.equals(result.matched)) {
                // One template matched a greater proportion of the resource
                // name than the other. Use the template which matched the most.
                return matched.length() > result.matched.length();
            } else if (mode != result.mode) {
                // Prefer equality match over startsWith match.
                return mode == RoutingMode.EQUALS;
            } else {
                // Prefer a match with less variables.
                return variableMap.size() < result.variableMap.size();
            }
        }

        @Override
        public Context decorateContext(Context context) {
            return new UriRouterContext(context, matched, remaining, variableMap);
        }
    }
}
