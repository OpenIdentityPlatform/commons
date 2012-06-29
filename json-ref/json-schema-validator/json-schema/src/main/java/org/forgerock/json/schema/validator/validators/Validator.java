/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright © 2011 ForgeRock AS. All rights reserved.
 * 
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 * 
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 * 
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 * $Id$
 */
package org.forgerock.json.schema.validator.validators;

import com.sun.org.apache.bcel.internal.generic.NEW;
import org.forgerock.json.fluent.JsonPointer;

import java.util.Map;

import static org.forgerock.json.schema.validator.Constants.REQUIRED;

/**
 * Validator is the abstract vase class of all typed validator.
 * <p/>
 * Each validator that responsible validate one certain type of object MUST extend this class.
 *
 * @author $author$
 * @version $Revision$ $Date$
 */
public abstract class Validator implements SimpleValidator<Object> {

    protected boolean required = false;

    public Validator(Map<String, Object> schema) {
        Object o = schema.get(REQUIRED);
        if (o instanceof Boolean) {
            required = ((Boolean) o);
        } else if (o instanceof String) {
            required = Boolean.parseBoolean((String) o);
        }
    }

    /**
     * Gets the valid JSONPath of the node or the given property.
     * <p/>
     * <p/>
     * Combines the two parameter and generates a valid JSONPath with dot–notation.
     * Simple type: $
     * Array type: $[0]
     * Object type: $.store.book[0].title
     *
     * @param at       JSONPath of the current node. If it's null then the value is {@code /}
     * @param property Property name of the child node.
     * @return JSONPath expressions uses the dot–notation
     *         Example: $.store.book[0].title
     */
    protected final JsonPointer getPath(JsonPointer at, String property) {
        JsonPointer path = null == at ? new JsonPointer() : at;
        if (null == property) {
            return path;
        } else {
            return path.child(property);
        }
    }

    public boolean isRequired() {
        return required;
    }
}
