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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.forgerock.api.models.ApiDescription;
import org.forgerock.api.models.ApiError;
import org.forgerock.api.models.Reference;
import org.forgerock.api.models.Resource;
import org.forgerock.api.models.Schema;
import org.forgerock.util.Reject;

/**
 * Helper that registers one or more {@link ApiDescription} instances and provides a means to resolve
 * {@link Reference}s.
 */
public class ReferenceResolver {

    private static final String DEFINITIONS_REF = "#/definitions/";

    private static final String ERRORS_REF = "#/errors/";

    private static final String SERVICES_REF = "#/services/";

    private final ApiDescription local;

    private final Map<String, ApiDescription> map;

    /**
     * Creates a reference-resolver and defines the one {@link ApiDescription} that can be used for local
     * (non-namespaced) reference lookups.
     *
     * @param local {@link ApiDescription} to use for local (non-namespaced) reference lookups
     */
    public ReferenceResolver(final ApiDescription local) {
        this.local = Reject.checkNotNull(local);
        map = new HashMap<>();
        register(local);
    }

    /**
     * Registers an external {@link ApiDescription}, for {@link org.forgerock.api.models.Reference} lookup, and
     * must not have previously been registered.
     *
     * @param apiDescription {@link ApiDescription} to register, which has not previously been registered
     * @return self
     */
    public ReferenceResolver register(final ApiDescription apiDescription) {
        if (map.containsKey(apiDescription.getId())) {
            throw new IllegalStateException("Already registered ID = " + apiDescription.getId());
        }
        map.put(apiDescription.getId(), apiDescription);
        return this;
    }

    /**
     * Registers external {@link ApiDescription}s, for {@link org.forgerock.api.models.Reference} lookup, and each
     * must not have previously been registered.
     *
     * @param apiDescriptions List of {@link ApiDescription}s to register, which have not previously been registered
     * @return self
     */
    public ReferenceResolver registerAll(final ApiDescription... apiDescriptions) {
        for (final ApiDescription item : apiDescriptions) {
            register(item);
        }
        return this;
    }

    /**
     * Gets a {@link org.forgerock.api.models.Definitions} {@link Schema} by JSON reference.
     *
     * @param reference JSON reference
     * @return {@link Schema} or {@code null} if not found
     */
    public Schema getDefinition(final Reference reference) {
        return resolveDefinition(reference, new HashSet<String>());
    }

    private Schema resolveDefinition(final Reference reference, final Set<String> visitedRefs) {
        final int nameStart = reference.getValue().indexOf(DEFINITIONS_REF);
        if (nameStart != -1) {
            final String name = reference.getValue().substring(nameStart + DEFINITIONS_REF.length());
            if (!name.isEmpty()) {
                if (!visitedRefs.add(reference.getValue())) {
                    throw new IllegalStateException("Reference loop detected: " + reference.getValue());
                }
                if (nameStart == 0) {
                    // there is no namespace, so do a local lookup
                    if (local.getDefinitions() != null) {
                        final Schema schema = local.getDefinitions().get(name);
                        if (schema != null && schema.getReference() != null) {
                            // reference chain
                            return resolveDefinition(schema.getReference(), visitedRefs);
                        }
                        return schema;
                    }
                } else {
                    final String namespace = reference.getValue().substring(0, nameStart);
                    final ApiDescription apiDescription = map.get(namespace);
                    if (apiDescription != null && apiDescription.getDefinitions() != null) {
                        final Schema schema = apiDescription.getDefinitions().get(name);
                        if (schema != null && schema.getReference() != null) {
                            // reference chain
                            return resolveDefinition(schema.getReference(), visitedRefs);
                        }
                        return schema;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Gets and {@link org.forgerock.api.models.Errors} {@link ApiError} by JSON reference.
     *
     * @param reference JSON reference
     * @return {@link ApiError} or {@code null} if not found
     */
    public ApiError getError(final Reference reference) {
        return resolveError(reference, new HashSet<String>());
    }

    private ApiError resolveError(final Reference reference, final Set<String> visitedRefs) {
        final int nameStart = reference.getValue().indexOf(ERRORS_REF);
        if (nameStart != -1) {
            final String name = reference.getValue().substring(nameStart + ERRORS_REF.length());
            if (!name.isEmpty()) {
                if (!visitedRefs.add(reference.getValue())) {
                    throw new IllegalStateException("Reference loop detected: " + reference.getValue());
                }
                if (nameStart == 0) {
                    // there is no namespace, so do a local lookup
                    if (local.getErrors() != null) {
                        final ApiError error = local.getErrors().get(name);
                        if (error != null && error.getReference() != null) {
                            // reference chain
                            return resolveError(error.getReference(), visitedRefs);
                        }
                        return error;
                    }
                } else {
                    final String namespace = reference.getValue().substring(0, nameStart);
                    final ApiDescription apiDescription = map.get(namespace);
                    if (apiDescription != null && apiDescription.getErrors() != null) {
                        final ApiError error = apiDescription.getErrors().get(name);
                        if (error != null && error.getReference() != null) {
                            // reference chain
                            return resolveError(error.getReference(), visitedRefs);
                        }
                        return error;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Get a {@link org.forgerock.api.models.Services} {@link Resource} by JSON reference.
     *
     * @param reference JSON reference
     * @return {@link Resource} or {@code null} if not found
     */
    public Resource getService(final Reference reference) {
        return resolveService(reference, new HashSet<String>());
    }

    private Resource resolveService(final Reference reference, final Set<String> visitedRefs) {
        final int nameStart = reference.getValue().indexOf(SERVICES_REF);
        if (nameStart != -1) {
            final String name = reference.getValue().substring(nameStart + SERVICES_REF.length());
            if (!name.isEmpty()) {
                if (!visitedRefs.add(reference.getValue())) {
                    throw new IllegalStateException("Reference loop detected: " + reference.getValue());
                }
                if (nameStart == 0) {
                    // there is no namespace, so do a local lookup
                    if (local.getServices() != null) {
                        final Resource service = local.getServices().get(name);
                        if (service != null && service.getReference() != null) {
                            // reference chain
                            return resolveService(service.getReference(), visitedRefs);
                        }
                        return service;
                    }
                } else {
                    final String namespace = reference.getValue().substring(0, nameStart);
                    final ApiDescription apiDescription = map.get(namespace);
                    if (apiDescription != null && apiDescription.getServices() != null) {
                        final Resource service = apiDescription.getServices().get(name);
                        if (service != null && service.getReference() != null) {
                            // reference chain
                            return resolveService(service.getReference(), visitedRefs);
                        }
                        return service;
                    }
                }
            }
        }
        return null;
    }
}
