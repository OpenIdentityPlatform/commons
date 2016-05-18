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

/**
 * Extension interface for CREST subclasses of Jackson's {@code JsonSchema} to specify a property as <em>required</em>
 * or optional.
 */
public interface RequiredSchema {

    /**
     * Get <em>required</em> field.
     *
     * @return {@code true} if property is required and {@code false} if optional
     */
    Boolean isRequired();

    /**
     * Set <em>required</em> field.
     *
     * @param required {@code true} if property is required and {@code false} if optional
     */
    void setRequired(Boolean required);
}
