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
 * Interface for common swagger model objects that support localizable titles and descriptions.
 * @param <T> The self-type of the model object.
 */
interface LocalizableTitleAndDescription<T> extends LocalizableDescription<T> {

    /**
     * Set the plain string title.
     * @param title The title.
     * @return This model object.
     */
    T title(String title);

    /**
     * Set the localizable title.
     * @param title The title.
     * @return This model object.
     */
    T title(LocalizableString title);

    /**
     * Get the localizable title for Jackson.
     * @return The title.
     */
    @JsonProperty("title")
    LocalizableString getLocalizableTitle();

    @JsonIgnore
    String getTitle();

}
