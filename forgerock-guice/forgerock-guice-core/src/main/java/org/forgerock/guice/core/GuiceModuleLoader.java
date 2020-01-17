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
 * Copyright 2014-2015 ForgeRock AS.
 */

package org.forgerock.guice.core;

import com.google.inject.Module;

import java.lang.annotation.Annotation;
import java.util.Set;

/**
 * <p>Implementations of this interface are responsible for finding and loading all Guice module classes which are
 * annotated with the provide module annotation.</p>
 *
 * <p>Implementations MUST only return subtypes of the Guice Module interface which are annotated with the provided
 * annotation. Any modules NOT annotated with the provided annotation MUST be ignored.</p>
 *
 * @since 1.0.0
 */
public interface GuiceModuleLoader {

    /**
     * Finds and loads all the Guice modules that will be used to configure the Guice injector instance.
     *
     * @param moduleAnnotation The Annotation that all modules MUST be annotated with.
     * @return A Set of Guice modules.
     */
    Set<Class<? extends Module>> getGuiceModules(Class<? extends Annotation> moduleAnnotation);
}
