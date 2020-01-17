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
 * Copyright 2014 ForgeRock AS
 */
package org.forgerock.doc.maven.utils.helper;

import freemarker.template.SimpleScalar;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModelException;
import org.forgerock.doc.maven.utils.NameUtils;

import java.util.List;

/**
 * FreeMarker method for getting a single document name.
 */
public class NameMethod implements TemplateMethodModelEx {

    @Override
    public Object exec(List args) throws TemplateModelException {
        if (args.size() != 4) {
            throw new TemplateModelException("Bad arguments list");
        }

        return NameUtils.renameDoc(
                ((SimpleScalar) args.get(0)).getAsString(),
                ((SimpleScalar) args.get(1)).getAsString(),
                ((SimpleScalar) args.get(2)).getAsString(),
                ((SimpleScalar) args.get(3)).getAsString());
    }
}
