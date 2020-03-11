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

import java.util.Objects;

import org.forgerock.util.i18n.LocalizableString;

import io.swagger.models.properties.Property;
import io.swagger.models.properties.StringProperty;

/**
 * Localizable {@link StringProperty}.
 */
class LocalizableStringProperty extends StringProperty implements LocalizableProperty<Property> {
    private LocalizableString title;
    private LocalizableString description;

    @Override
    public LocalizableStringProperty title(LocalizableString title) {
        this.title = title;
        return this;
    }

    @Override
    public LocalizableStringProperty description(LocalizableString desc) {
        this.description = desc;
        return this;
    }

    @Override
    public LocalizableStringProperty title(String title) {
        setTitle(title);
        return this;
    }

    @Override
    public void setTitle(String title) {
        super.setTitle(title);
        this.title = new LocalizableString(title);
    }

    @Override
    public LocalizableStringProperty description(String description) {
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
    public boolean equals(final Object o) {
        if (!super.equals(o)) {
            return false;
        }
        if (!(o instanceof LocalizableStringProperty)) {
            return false;
        }
        final LocalizableStringProperty other = (LocalizableStringProperty) o;
        if (!Objects.equals(title, other.title)) {
            return false;
        }
        if (!Objects.equals(description, other.description)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), title, description);
    }

}
