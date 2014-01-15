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
 * Copyright 2013-2014 ForgeRock AS.
 */

package org.forgerock.jaspi.test.server.endpoint.one;

import org.forgerock.jaspi.runtime.context.config.ModuleConfigurationFactory;
import org.forgerock.json.fluent.JsonValue;

public class EndpointModuleConfigurationFactory implements ModuleConfigurationFactory {

    public static ModuleConfigurationFactory getModuleConfigurationFactory() {
        return new EndpointModuleConfigurationFactory();
    }

    @Override
    public JsonValue getConfiguration() {
        return JsonValue.json(
            JsonValue.object(
                JsonValue.field(SERVER_AUTH_CONTEXT_KEY, JsonValue.object(
                    JsonValue.field(SESSION_MODULE_KEY, JsonValue.object(
                        JsonValue.field(AUTH_MODULE_CLASS_NAME_KEY, "org.forgerock.jaspi.web.SessionAuthModule"),
                        JsonValue.field(AUTH_MODULE_PROPERTIES_KEY, JsonValue.object(
                            JsonValue.field("keyAlias", "openidm-localhost")
                        ))
                    )),
                    JsonValue.field(AUTH_MODULES_KEY, JsonValue.array(
                        JsonValue.object(
                            JsonValue.field(AUTH_MODULE_CLASS_NAME_KEY, "org.forgerock.jaspi.web.AuthModuleOne"),
                            JsonValue.field(AUTH_MODULE_PROPERTIES_KEY, JsonValue.object(
                                JsonValue.field("passThroughAuth", "system/AD/account")
                            ))
                        ),
                        JsonValue.object(
                            JsonValue.field(AUTH_MODULE_CLASS_NAME_KEY, "org.forgerock.jaspi.web.AuthModuleTwo"),
                            JsonValue.field(AUTH_MODULE_PROPERTIES_KEY, JsonValue.object(
                                JsonValue.field("passThroughAuth", "system/AD/account")
                            ))
                        )
                    ))
                ))
            )
        );
    }
}
