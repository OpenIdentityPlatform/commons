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

package org.forgerock.api;

import java.io.InputStream;
import java.io.InputStreamReader;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.JavaScriptException;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.Scriptable;
import org.testng.annotations.Test;

/**
 * Tests using Javascript with API Descriptor's Builder API.
 */
public class JavascriptTest {

    @Test
    public void testSimpleExample() throws Exception {
        executeScript("SimpleExample", "/com/forgerock/api/SimpleExample.js");
    }

    private void executeScript(String name, final String resourcePath) throws Exception {
        final InputStream stream = JavascriptTest.class.getResourceAsStream(resourcePath);
        Context context = Context.enter();
        try {
            Scriptable scope = context.initStandardObjects();
            context.evaluateReader(scope, new InputStreamReader(stream), name, 1, null);
        } catch (JavaScriptException e) {
            String message = e.sourceName() + ":" + e.lineNumber();
            if (e.getValue() instanceof NativeObject) {
                message += ": " + ((NativeObject) e.getValue()).get("message").toString();
            }
            throw new Exception(message);
        } finally {
            Context.exit();
        }
    }

}
