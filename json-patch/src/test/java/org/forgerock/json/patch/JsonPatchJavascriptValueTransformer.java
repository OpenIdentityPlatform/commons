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
 * information: "Portions Copyrighted [year] [name of copyright owner]".
 *
 * Copyright 2015 ForgeRock AS. All rights reserved.
 */

package org.forgerock.json.patch;

import org.forgerock.json.fluent.JsonPointer;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.fluent.JsonValueException;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

/**
 * RFC6902 expects the patch value to be a predetermined, static value to be used in the
 * patch operation's execution.  This class provides an alternate approach using javascript
 * to potentially transform a value (or the entire document itself).
 */
public class JsonPatchJavascriptValueTransformer implements JsonPatchValueTransformer {
    /**
     * Path to the "script" attribute of a patch entry. This attribute may be used in
     * place of the "value" attribute to provide a javascript value transform. If both
     * "script" and "value" are present then "value" takes precedence.
     */
    private static final JsonPointer scriptPtr = new JsonPointer("/script");

    /**
     * Return the value to be used for a given patch operation, transformed via javascript
     * if applicable.
     *
     * The target document is made available to javascript through a variable named
     * 'content'.
     *
     * @param target the patch target document.  Target is unused by default, made available
     *               for use by custom transforms.
     * @param op the patch operation.
     * @return
     * @throws org.forgerock.json.fluent.JsonValueException
     */
    public Object getTransformedValue(JsonValue target, JsonValue op) throws JsonValueException {
        if (op.get(JsonPatch.valuePtr) != null) {
            return op.get(JsonPatch.valuePtr).getObject();
        } else if (op.get(scriptPtr) != null) {
            return evalScript(target, op.get(scriptPtr));
        } else {
            throw new JsonValueException(op, "expecting a value or script member");
        }
    }

    private String evalScript(JsonValue content, JsonValue script) {
        if (script == null || script.getObject() == null || !script.isString()) {
            return null;
        }
        Context cx = Context.enter();
        try {
            Scriptable scope = cx.initStandardObjects();
            String finalScript = "var content = " + content.toString() + "; " + script.getObject();
            Object result = cx.evaluateString(scope, finalScript, "script", 1, null);
            return Context.toString(result);
        } catch (Exception e) {
            throw new JsonValueException(script, "failed to eval script", e);
        } finally {
            Context.exit();
        }
    }
}

