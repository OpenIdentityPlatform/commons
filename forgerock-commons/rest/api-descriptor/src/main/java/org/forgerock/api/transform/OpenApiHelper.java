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

package org.forgerock.api.transform;

import io.swagger.models.Operation;
import io.swagger.models.Path;
import io.swagger.models.Swagger;
import io.swagger.models.parameters.HeaderParameter;
import io.swagger.models.parameters.RefParameter;

/**
 * Helper methods for applying commonly needed changes to the {@link io.swagger.models.Swagger} model.
 */
public final class OpenApiHelper {

    private OpenApiHelper() {
        // hidden
    }

    /**
     * Adds a header to all operations. For example, one may need to add authentication headers for
     * username and password.
     *
     * @param header Header model
     * @param swagger Swagger model
     */
    public static void addHeaderToAllOperations(final HeaderParameter header, final Swagger swagger) {
        final String headerKey = header.getName() + "_header";
        if (swagger.getParameter(headerKey) != null) {
            throw new IllegalStateException("Header already exists with name: " + header.getName());
        }

        swagger.addParameter(headerKey, header);
        final RefParameter refParameter = new RefParameter(headerKey);

        for (final Path path : swagger.getPaths().values()) {
            for (final Operation operation : path.getOperations()) {
                operation.addParameter(refParameter);
            }
        }
    }

    /**
     * Visits all operations.
     *
     * @param visitor Operation visitor
     * @param swagger Swagger model
     */
    public static void visitAllOperations(final OperationVisitor visitor, final Swagger swagger) {
        for (final Path path : swagger.getPaths().values()) {
            for (final Operation operation : path.getOperations()) {
                visitor.visit(operation);
            }
        }
    }

    /**
     * Visits a Swagger {@code Operation}.
     */
    public interface OperationVisitor {
        /**
         * Visits a Swagger {@code Operation}.
         *
         * @param operation Operation
         */
        void visit(Operation operation);
    }
}
