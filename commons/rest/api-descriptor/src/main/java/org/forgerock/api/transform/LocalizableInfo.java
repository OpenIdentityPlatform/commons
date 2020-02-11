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

import io.swagger.models.Info;

/**
 * A localizable {@code Info}.
 */
class LocalizableInfo extends Info implements LocalizableTitleAndDescription<Info> {

    private LocalizableString title;
    private LocalizableString description;

    @Override
    public LocalizableInfo title(LocalizableString title) {
        this.title = title;
        return this;
    }

    @Override
    public LocalizableInfo description(LocalizableString description) {
        this.description = description;
        return this;
    }

    @Override
    public LocalizableInfo title(String title) {
        setTitle(title);
        return this;
    }

    @Override
    public void setTitle(String title) {
        super.setTitle(title);
        this.title = new LocalizableString(title);
    }

    @Override
    public LocalizableInfo description(String description) {
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

    @Override
    public LocalizableInfo mergeWith(Info info) {
        super.mergeWith(info);
        if (info instanceof LocalizableInfo) {
            LocalizableInfo localizableInfo = (LocalizableInfo) info;
            if (localizableInfo.description != null) {
                this.description = localizableInfo.description;
            }
            if (localizableInfo.title != null) {
                this.title = localizableInfo.title;
            }
        } else {
            if (info.getDescription() != null) {
                description(info.getDescription());
            }
            if (info.getTitle() != null) {
                title(info.getTitle());
            }
        }
        return this;
    }
}
