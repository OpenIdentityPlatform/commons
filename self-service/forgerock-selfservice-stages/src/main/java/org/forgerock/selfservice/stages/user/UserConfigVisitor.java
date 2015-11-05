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

package org.forgerock.selfservice.stages.user;

import org.forgerock.selfservice.core.ProgressStageBinder;
import org.forgerock.selfservice.core.config.StageConfigVisitor;

/**
 * Visitor that builds a user details stages using visited user details config.
 *
 * @since 0.3.0
 */
public interface UserConfigVisitor extends StageConfigVisitor {

    /**
     * Builds a user details stage bound to the user deatils config.
     *
     * @param config
     *         user details config
     *
     * @return user details stage binding
     */
    ProgressStageBinder<?> build(UserDetailsConfig config);

    /**
     * Builds a user query stage bound to the user query config.
     *
     * @param config
     *         user query config
     *
     * @return user query stage binding
     */
    ProgressStageBinder<?> build(UserQueryConfig config);

    /**
     * Builds a username retrieve stage bound to the username retrieve config.
     *
     * @param config
     *         username retrieve config
     *
     * @return username retrieve stage binding
     */
    ProgressStageBinder<?> build(RetrieveUsernameConfig config);

}
