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
 * Copyright 2016 ForgeRock AS.
 */
package org.forgerock.api.util;

import static org.forgerock.api.util.ValidationUtil.isEmpty;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.forgerock.api.enums.ParameterSource;
import org.forgerock.api.models.Parameter;

/** Utilities for working with API Description paths and path-parameters. */
public final class PathUtil {

    /** Pattern for replacing multiple forward-slashes with a single forward-slash. */
    private static final Pattern SQUASH_FORWARD_SLASHES_PATTERN = Pattern.compile("[/]{2,}");
    /** Pattern for removing multiple trailing-slashes. */
    private static final Pattern TRAILING_SLASHES_PATTERN = Pattern.compile("[/]+$");
    /** Pattern for finding curly-brace-delimited path-variables in a URL-path. */
    private static final Pattern PATH_VARIABLE_PATTERN = Pattern.compile("\\{([^{}]+)\\}");

    private PathUtil() {
        // empty
    }

    /**
     * Builds a forward-slash-delimited path, with duplicate forward-slashes removed, and trailing slashes removed.
     *
     * @param segment First path segment
     * @param moreSegments Additional path segments or {@code null}
     * @return Path
     */
    public static String buildPath(final String segment, final String... moreSegments) {
        if (isEmpty(segment)) {
            throw new IllegalArgumentException("segment argument required");
        }
        final StringBuilder path = new StringBuilder().append('/').append(segment);
        if (moreSegments != null) {
            for (final String s : moreSegments) {
                path.append('/').append(s);
            }
        }

        // squash forward-slashes
        final Matcher m = SQUASH_FORWARD_SLASHES_PATTERN.matcher(path);
        final String normalized = m.find() ? m.replaceAll("/") : path.toString();

        // remove trailing-slashes
        return TRAILING_SLASHES_PATTERN.matcher(normalized).replaceAll("");
    }

    /**
     * Searches for curly-braces in the given {@code pathSegment}, and creates a path-parameter for each that are found.
     *
     * @param pathSegment Path-segment
     * @return Path-parameters or {@code null}
     */
    public static Parameter[] buildPathParameters(final String pathSegment) {
        if (!isEmpty(pathSegment)) {
            final Matcher m = PATH_VARIABLE_PATTERN.matcher(pathSegment);
            if (m.find()) {
                final List<Parameter> parameters = new ArrayList<>();
                int start = 0;
                while (m.find(start)) {
                    parameters.add(Parameter.parameter()
                            .name(m.group(1))
                            .type("string")
                            .source(ParameterSource.PATH)
                            .required(true)
                            .build());
                    start = m.end();
                }
                return parameters.toArray(new Parameter[parameters.size()]);
            }
        }
        return null;
    }

    /**
     * Merges {@link Parameter} values into the given {@code parameterList}, where conflicting
     * {@link Parameter#getName() parameter-names} will be replaced, and new parameters will otherwise be added.
     *
     * @param parameterList Current list of parameters
     * @param parameters Additional parameters to merge or {@code null}
     * @return {@code parameterList} field
     */
    public static List<Parameter> mergeParameters(final List<Parameter> parameterList,
            final Parameter... parameters) {
        if (parameters != null) {
            for (final Parameter parameter : parameters) {
                // replace parameter if name already exists, otherwise add parameter to end of list
                int replaceIndex = -1;
                for (int i = 0; i < parameterList.size(); ++i) {
                    if (parameterList.get(i).getName().equals(parameter.getName())) {
                        replaceIndex = i;
                        break;
                    }
                }
                if (replaceIndex != -1) {
                    parameterList.set(replaceIndex, parameter);
                } else {
                    parameterList.add(parameter);
                }
            }
        }
        return parameterList;
    }
}
