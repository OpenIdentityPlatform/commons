/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013-2014 ForgeRock AS. All Rights Reserved
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
 */

package org.forgerock.script;

import org.forgerock.services.context.AbstractContext;
import org.forgerock.services.context.Context;
import org.forgerock.services.context.ClientContext;
import org.forgerock.json.JsonValue;

/**
 * A context to wrap the calling context when entering a script.
 */
public class ScriptContext extends AbstractContext {
    private static final String ATTR_SCRIPT_NAME = "scriptName";
    private static final String ATTR_SCRIPT_TYPE = "scriptType";
    private static final String ATTR_SCRIPT_REVISION = "scriptRevision";

    public ScriptContext(Context parent, String name, String type, String revision) {
        // add an internal context when entering a script
        super(ClientContext.newInternalClientContext(parent), "script");
        data.put(ATTR_SCRIPT_NAME, name);
        data.put(ATTR_SCRIPT_TYPE, type);
        data.put(ATTR_SCRIPT_REVISION, revision);
    }

    public ScriptContext(JsonValue savedContext, ClassLoader classLoader) {
        super(savedContext, classLoader);
    }

    public String getName() {
        return data.get(ATTR_SCRIPT_NAME).asString();
    }

    public String getType() {
        return data.get(ATTR_SCRIPT_TYPE).asString();
    }

    public String getRevision() {
        return data.get(ATTR_SCRIPT_REVISION).asString();
    }
}
