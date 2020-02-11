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
 * Copyright 2011-2016 ForgeRock AS.
 */

package org.forgerock.json.schema.validator.helpers;

import org.forgerock.json.JsonPointer;
import org.forgerock.json.schema.validator.ErrorHandler;
import org.forgerock.json.schema.validator.exceptions.SchemaException;
import org.forgerock.json.schema.validator.validators.SimpleValidator;

/**
 * This class implements "format" validation on primitive types of objects as defined in
 * the paragraph 5.23 of the JSON Schema specification.
 * <p/>
 * Additional custom formats MAY be created.  These custom formats MAY
 * be expressed as an URI, and this URI MAY reference a schema of that
 * format.
 *
 * @see <a href="http://tools.ietf.org/html/draft-zyp-json-schema-03#section-5.23">format</a>
 */
public class FormatHelper implements SimpleValidator<Object> {

    /**
     * Construct a new format helper.
     * @param format The format.
     */
    public FormatHelper(String format) {
    }

    @Override
    public void validate(Object node, JsonPointer at, ErrorHandler handler) throws SchemaException {
        //TODO: implements
    }
}
