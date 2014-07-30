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
 * Copyright 2014 ForgeRock AS.
 */

package org.forgerock.caf.authn;

/**
 * Object containing the expected audit operations to have been performed.
 *
 * @since 1.5.0
 */
final class AuditParameters {

    private final String expectedOutcome;

    private AuditParameters(String expectedOutcome) {
        this.expectedOutcome = expectedOutcome;
    }

    /**
     * Creates a new {@code AuditParameters} object with the given parameters.
     *
     * @param expectedOutcome The expected audit outcome of the request.
     * @return An {@code AuditParameters} object/
     */
    static AuditParameters auditParams(String expectedOutcome) {
        return new AuditParameters(expectedOutcome);
    }

    public String expectedOutcome() {
        return expectedOutcome;
    }
}
