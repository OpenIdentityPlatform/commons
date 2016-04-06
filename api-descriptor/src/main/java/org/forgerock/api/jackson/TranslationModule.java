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

package org.forgerock.api.jackson;

import com.fasterxml.jackson.databind.module.SimpleModule;

/**
 * Custom SimpleModule implementation that allows registration of serializers and deserializers.
 */
public class TranslationModule extends SimpleModule {

    /**
     * Default constructor.
     */
    public TranslationModule() {
        super();
    }

    /**
     * Method called by ObjectMapper when module is registered.
     * It is called to let module register functionality it provides,
     * using callback methods passed-in context object exposes.
     * @param context The Setup context
     */
    public void setupModule(SetupContext context) {
        super.setupModule(context);
        context.addBeanSerializerModifier(new ResourceSerializerModifier());
    }
}
