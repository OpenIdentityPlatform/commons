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

package org.forgerock.api.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.ElementType;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.forgerock.api.enums.ReadPolicy;
import org.forgerock.api.enums.WritePolicy;

/**
 * An annotation to declare the policies for property access in the CREST API Descriptor schema elements.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD})
public @interface PropertyPolicies {
    /**
     * The write policy for the property. Defaults to {@code WRITABLE}.
     */
    WritePolicy write() default WritePolicy.WRITABLE;

    /**
     * The read policy for the property. Defaults to {@code USER}.
     */
    ReadPolicy read() default ReadPolicy.USER;

    /**
     * Whether breaking the declared policy will result in an error from the service. Only required when
     * {@link #write()} is not set to {@code WRITABLE}. Defaults to {@code false}.
     */
    boolean errorOnWritePolicyFailure() default false;

    /**
     * {@code true} when a field is available, but must be explicitly requested, or {@code false} (default) when
     * always returned.
     */
    boolean returnOnDemand() default false;
}
