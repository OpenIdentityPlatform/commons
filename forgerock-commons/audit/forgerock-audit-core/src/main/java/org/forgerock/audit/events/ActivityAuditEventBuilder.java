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
package org.forgerock.audit.events;

/**
 * Builder for audit activity events.
 * <p>
 * This builder should not be used directly but be specialized for each product to allow to define
 * new specific fields, e.g
 * <pre>
 * <code>
 * class OpenProductActivityAuditEventBuilder{@code <T extends OpenProductActivityAuditEventBuilder<T>>}
 *         extends ActivityAuditEventBuilder{@code <T>} {
 *
 *     public static {@code <T>} OpenProductActivityAuditEventBuilder{@code <?>} productActivityEvent() {
 *         return new OpenProductActivityAuditEventBuilder();
 *     }
 *
 *     public T someField(String v) {
 *         jsonValue.put("someField", v);
 *         return self();
 *     }
 *
 *    ...
 * }
 * </code>
 * </pre>
 *
 * @param <T> the type of the builder
 */
public class ActivityAuditEventBuilder<T extends ActivityAuditEventBuilder<T>> extends StateChangeAuditEventBuilder<T> {

    /**
     * Creates the builder.
     */
    protected ActivityAuditEventBuilder() {
        // Reduce visibility of the default constructor
    }

    /**
     * Starts to build an audit activity event.
     * <p>
     * Note: it is preferable to use a specialized builder that allow to add
     * fields specific to a product.
     *
     * @return an audit activity event builder
     */
    @SuppressWarnings("rawtypes")
    public static ActivityAuditEventBuilder<?> activityEvent() {
        return new ActivityAuditEventBuilder();
    }

}
