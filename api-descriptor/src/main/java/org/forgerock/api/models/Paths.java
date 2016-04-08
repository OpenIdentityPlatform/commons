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

package org.forgerock.api.models;

import static org.forgerock.api.util.ValidationUtil.containsWhitespace;
import static org.forgerock.api.util.ValidationUtil.isEmpty;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonValue;
import org.forgerock.api.ApiValidationException;
import org.forgerock.util.Reject;

/**
 * Class that represents the Paths type in API descriptor.
 *
 * @param <T> Type implements {@link PathNode}
 */
public final class Paths<T extends PathNode> {

    private final Map<String, T> paths;

    private Paths(Builder<T> builder) {
        this.paths = builder.paths;

        if (paths.isEmpty()) {
            throw new ApiValidationException("Must have at least one path definition");
        }
    }

    /**
     * Gets a {@code Map} of path-names to Paths. This method is currently only used for JSON serialization.
     *
     * @return {@code Map} of path-names to Paths.
     */
    @JsonValue
    protected Map<String, T> getPaths() {
        return paths;
    }

    /**
     * Gets the Path for a given Path-name.
     *
     * @param name Path name
     * @return Path or {@code null} if does-not-exist.
     */
    @JsonIgnore
    public T get(String name) {
        return paths.get(name);
    }

    /**
     * Returns all Path names.
     *
     * @return All Path names.
     */
    @JsonIgnore
    public Set<String> getNames() {
        return paths.keySet();
    }

    /**
     * Create a new Builder for Paths.
     *
     * @param <T3> Type implements {@link PathNode}
     * @param pathNodeClass {@code Class} of the {@link PathNode} implementation being used
     * @return Builder
     */
    public static <T3 extends PathNode> Builder<T3> paths(Class<T3> pathNodeClass) {
        return new Builder<>();
    }

    /**
     * Builder to help construct the Paths.
     *
     * @param <T2> Type implements {@link PathNode}
     */
    public static final class Builder<T2 extends PathNode> {

        private final Map<String, T2> paths = new HashMap<>();

        /**
         * Private default constructor.
         */
        private Builder() {
        }

        /**
         * Adds a Path.
         *
         * @param name Path name
         * @param path Path
         * @return Builder
         */
        public Builder<T2> put(String name, T2 path) {
            if (isEmpty(name) || containsWhitespace(name)) {
                throw new IllegalArgumentException("name required and may not contain whitespace");
            }
            if (paths.containsKey(name)) {
                throw new IllegalStateException("name not unique");
            }
            paths.put(name, Reject.checkNotNull(path));
            return this;
        }

        /**
         * Builds the Paths instance.
         *
         * @return Paths instance
         */
        public Paths<T2> build() {
            return new Paths<>(this);
        }
    }

}