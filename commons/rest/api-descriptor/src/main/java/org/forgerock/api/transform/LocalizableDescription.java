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

import org.forgerock.util.i18n.LocalizableString;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A localizable model object with a description.
 * @param <T> The self-type reference.
 */
interface LocalizableDescription<T> {
    /**
     * Set the localizable description.
     * @param description The title.
     * @return This model object.
     */
    T description(LocalizableString description);

    /**
     * Set the plain string description.
     * @param description The title.
     * @return This model object.
     */
    T description(String description);

    /**
     * Get the localizable title for Jackson.
     * @return The title.
     */
    @JsonProperty("description")
    LocalizableString getLocalizableDescription();

    @JsonIgnore
    String getDescription();

}
