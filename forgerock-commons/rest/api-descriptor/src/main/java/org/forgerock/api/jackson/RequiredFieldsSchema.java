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

package org.forgerock.api.jackson;

import java.util.Set;

/**
 * Extension interface for CREST subclasses of Jackson's {@code JsonSchema} to specify an array of <em>required</em>
 * fields.
 */
interface RequiredFieldsSchema {

    /**
     * Get <em>required</em> field names.
     *
     * @return Required field-names or {@code null}
     */
    Set<String> getRequiredFields();

    /**
     * Set <em>required</em> field names.
     *
     * @param requiredFields Required field-names or {@code null}
     */
    void setRequiredFields(Set<String> requiredFields);
}
