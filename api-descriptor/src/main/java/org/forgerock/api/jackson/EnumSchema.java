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

import java.util.List;

/**
 * Extension interface for CREST subclasses of Jackson's {@code JsonSchema} to specify the enum titles.
 */
interface EnumSchema {
    String ENUM_TITLES = "enum_titles";

    /**
     * Get the list of enum titles.
     * @return The titles.
     */
    List<String> getEnumTitles();
    /**
     * Set the list of enum titles.
     * @param titles The titles.
     */
    void setEnumTitles(List<String> titles);
}
