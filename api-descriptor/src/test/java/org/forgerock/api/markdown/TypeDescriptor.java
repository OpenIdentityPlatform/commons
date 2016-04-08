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

package org.forgerock.api.markdown;

import java.util.ArrayList;
import java.util.List;

/**
 * Holds a record of the properties table in the markdown
 */
public final class TypeDescriptor {

    private final String name;
    private String superClass;
    private List<PropertyRecord> properties = new ArrayList<PropertyRecord>();

    public TypeDescriptor(String name) {
        this.name = name;
    }

    public String getSuperClass() {
        return superClass;
    }

    public void setSuperClass(String superClass) {
        this.superClass = superClass;
    }

    public List<PropertyRecord> getProperties() {
        return properties;
    }

    public void setProperties(List<PropertyRecord> properties) {
        this.properties = properties;
    }

    public void addProperty(PropertyRecord propertyRecord) {
        this.properties.add(propertyRecord);
    }

    public String getName() {
        return name;
    }

}
