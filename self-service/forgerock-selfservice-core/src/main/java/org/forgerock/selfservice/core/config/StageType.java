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

package org.forgerock.selfservice.core.config;

/**
 * The stage type provides some relationship between a stage config and a progress stage.
 *
 * @since 0.1.0
 */
public final class StageType<C extends StageConfig> {

    private final String name;
    private final Class<C> configClass;

    private StageType(String name, Class<C> configClass) {
        this.name = name;
        this.configClass = configClass;
    }

    public String getName() {
        return name;
    }

    /**
     * Given a generic stage config returns its typed representation.
     *
     * @param config
     *         a generic stage config
     *
     * @return a typed stage config
     */
    public C getTypedConfig(StageConfig config) {
        return configClass.cast(config);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof StageType)) {
            return false;
        }

        StageType<?> that = (StageType<?>) o;
        if (name == null ? that.name != null : !name.equals(that.name)) {
            return false;
        }

        if (configClass == null ? that.configClass != null : !configClass.equals(that.configClass)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (configClass != null ? configClass.hashCode() : 0);
        return result;
    }

    /**
     * Retrieves the stage type representation of the passed name and stage config class.
     *
     * @param name
     *         the stage type name
     * @param configClass
     *         the stage config class
     * @param <C>
     *         the typed representation of the stage config
     *
     * @return a stage type instance
     */
    public static <C extends StageConfig> StageType<C> valueOf(String name, Class<C> configClass) {
        return new StageType<>(name, configClass);
    }

}
