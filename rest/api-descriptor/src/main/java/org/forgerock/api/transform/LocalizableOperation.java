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

import java.util.ArrayList;
import java.util.List;

import org.forgerock.util.i18n.LocalizableString;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.models.Operation;

/**
 * Localizable {@link Operation}.
 */
public class LocalizableOperation extends Operation implements LocalizableDescription<Operation> {
    private LocalizableString description;
    private List<LocalizableString> tags;

    @Override
    public LocalizableOperation description(LocalizableString desc) {
        this.description = desc;
        return this;
    }

    @Override
    public LocalizableOperation description(String description) {
        setDescription(description);
        return this;
    }

    @Override
    public void setDescription(String description) {
        super.setDescription(description);
        this.description = new LocalizableString(description);
    }

    @Override
    public LocalizableString getLocalizableDescription() {
        return description;
    }

    @Override
    public void addTag(String tag) {
        super.addTag(tag);
        addTag(new LocalizableString(tag));
    }

    public void addTag(LocalizableString tag) {
        if (tags == null) {
            tags = new ArrayList<>();
        }
        tags.add(tag);
    }

    @Override
    public void setTags(List<String> tags) {
        super.setTags(tags);
        tags = new ArrayList<>();
        for (String tag : tags) {
            addTag(tag);
        }
    }

    @JsonProperty("tags")
    public List<LocalizableString> getLocalizableTags() {
        return tags;
    }

    @Override
    @JsonIgnore
    public List<String> getTags() {
        return super.getTags();
    }
}
