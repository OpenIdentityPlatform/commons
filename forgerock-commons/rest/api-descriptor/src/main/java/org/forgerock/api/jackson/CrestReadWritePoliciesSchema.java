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

import org.forgerock.api.enums.ReadPolicy;
import org.forgerock.api.enums.WritePolicy;

/**
 * Extension interface for CREST subclasses of Jackson's {@code JsonSchema} to specify the field read and write
 * policies.
 */
interface CrestReadWritePoliciesSchema {

    /**
     * Get the write policy for the property.
     *
     * @return The write policy.
     */
    WritePolicy getWritePolicy();

    /**
     * Set the write policy for the property.
     *
     * @param policy The write policy.
     */
    void setWritePolicy(WritePolicy policy);

    /**
     * Get the read policy for the property.
     *
     * @return The read policy.
     */
    ReadPolicy getReadPolicy();

    /**
     * Set the read policy for the property.
     *
     * @param policy The read policy.
     */
    void setReadPolicy(ReadPolicy policy);

    /**
     * Get the error indicator for failed write policy.
     *
     * @return Whether errors will be returned.
     */
    Boolean getErrorOnWritePolicyFailure();

    /**
     * Set the error indicator for failed write policy.
     *
     * @param errorOnWritePolicyFailure Whether errors will be returned.
     */
    void setErrorOnWritePolicyFailure(Boolean errorOnWritePolicyFailure);

    /**
     * Set the return-on-demand field.
     *
     * @return {@code true} when a field is available, but must be explicitly requested, or {@code false} (default) when
     * always returned.
     */
    Boolean getReturnOnDemand();

    /**
     * Get the return-on-demand field.
     *
     * @param returnOnDemand {@code true} when a field is available, but must be explicitly requested, or
     * {@code false} (default) when always returned.
     */
    void setReturnOnDemand(Boolean returnOnDemand);
}
