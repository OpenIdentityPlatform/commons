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

import io.swagger.models.ComposedModel;

/** Localizable {@link ComposedModel}. */
class LocalizableComposedModel extends ComposedModel
                               implements LocalizableTitleAndDescription<LocalizableComposedModel> {
    private LocalizableString title;
    private LocalizableString description;

    @Override
    public LocalizableComposedModel title(String title) {
        setTitle(title);
        return this;
    }

    @Override
    public LocalizableComposedModel title(LocalizableString title) {
        this.title = title;
        return this;
    }

    @Override
    public LocalizableComposedModel description(LocalizableString description) {
        this.description = description;
        return this;
    }

    @Override
    public void setTitle(String title) {
        super.setTitle(title);
        this.title = new LocalizableString(title);
    }

    @Override
    public LocalizableComposedModel description(String description) {
        setDescription(description);
        return this;
    }

    @Override
    public void setDescription(String description) {
        super.setDescription(description);
        this.description = new LocalizableString(description);
    }

    @Override
    public LocalizableString getLocalizableTitle() {
        return title;
    }

    @Override
    public LocalizableString getLocalizableDescription() {
        return description;
    }
}
