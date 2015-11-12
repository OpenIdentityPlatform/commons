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
 * Copyright 2015 ForgeRock AS.
 */
package org.forgerock.selfservice.stages.kba;

import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.selfservice.core.ProgressStage;
import org.forgerock.selfservice.core.crypto.CryptoService;

/**
 * Base class for KBA stages.
 *
 * @since 0.2.0
 */
abstract class AbstractKbaStage<C extends AbstractKbaStageConfig<?>> implements ProgressStage<C> {

    protected static final String REQUIREMENT_PROPERTY_ANSWER = "answer";
    protected static final String REQUIREMENT_PROPERTY_ID = "id";
    protected static final String REQUIREMENT_PROPERTY_QUESTION = "question";
    protected static final String REQUIREMENT_PROPERTY_SYSTEM_QUESTION = "systemQuestion";
    protected static final String REQUIREMENT_PROPERTY_USER_QUESTION = "userQuestion";
    protected static final String REQUIREMENT_PROPERTY_CUSTOM_QUESTION = "customQuestion";
    protected static final String REQUIREMENT_PROPERTY_QUESTION_ID = "questionId";

    protected final ConnectionFactory connectionFactory;

    protected final CryptoService cryptoService;

    AbstractKbaStage(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
        this.cryptoService = new CryptoService();
    }

}
