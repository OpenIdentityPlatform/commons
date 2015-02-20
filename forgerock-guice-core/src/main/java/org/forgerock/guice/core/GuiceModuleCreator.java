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
 * Copyright 2013-2015 ForgeRock AS.
 */

package org.forgerock.guice.core;

import com.google.inject.Module;
import org.forgerock.guice.core.internal.commons.lang.reflect.ConstructorUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;

/**
 * <p>Responsible for creating instances of Guice modules.</p>
 *
 * <p>All Guice modules must have one and only one public no-arg constructor.</p>
 *
 * <p>This class must be able to operate before Guice is initialised, as it is used to initialise Guice.</p>
 *
 * @since 1.0.0
 */
class GuiceModuleCreator {

    private final Logger logger = LoggerFactory.getLogger(InjectorFactory.class);

    /**
     * Creates an instance of the Guice module class.
     *
     * @param clazz The Guice module class.
     * @param <T> The Guice module class type.
     * @return A non-null instance of the Guice module.
     */
    <T extends Module> T createInstance(Class<T> clazz) {
        try {
            final Constructor<T> constructor = getConstructor(clazz);
            return constructor.newInstance();
        } catch (Exception e) {
            if (logger.isErrorEnabled()) {
                logger.error("An exception occurred when trying to instantiate class, " + clazz.getName(), e);
            }
            throw new ModuleCreationException(e);
        }
    }

    /**
     * <p>Finds and returns the Constructor to use to create the Guice module.</p>
     *
     * <p>Note: There must be one and only one public no-arg constructor for the Guice module.</p>
     *
     * @param clazz The Guice module class.
     * @param <T> The Guice module class type.
     * @return The public no-arg constructor.
     * @throws NoSuchMethodException If no public no-arg constructor exists in this class.
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private <T> Constructor<T> getConstructor(Class<T> clazz) throws NoSuchMethodException {

        Constructor constructor = ConstructorUtils.getAccessibleConstructor(clazz, new Class[]{});

        if (constructor != null) {
            return constructor;
        } else {
            throw new NoSuchMethodException(String.format("No public zero-arg constructor found on %s",
                clazz.getCanonicalName()));
        }
    }
}
