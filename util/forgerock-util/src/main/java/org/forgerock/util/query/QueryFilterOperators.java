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
package org.forgerock.util.query;

/**
 * QueryFilter constants.
 */
public final class QueryFilterOperators {
    /** the "and" operator. */
    public static final String AND = "and";
    /** the "or" operator. */
    public static final String NOT = "!";
    /** the "not" operator. */
    public static final String OR = "or";
    /** a literal "true". */
    public static final String TRUE = "true";
    /** a literal "false. */
    public static final String FALSE = "false";
    /** the "present" operator. */
    public static final String PRESENT = "pr";
    /** the "equals" operator. */
    public static final String EQUALS = "eq";
    /** the "greater-than" operator. */
    public static final String GREATER_THAN = "gt";
    /** the "greater-than-or-equal" operator. */
    public static final String GREATER_EQUAL = "ge";
    /** the "less-than" operator. */
    public static final String LESS_THAN = "lt";
    /** the "less-than-or-equal" operator. */
    public static final String LESS_EQUAL = "le";
    /** the "contains" operator". */
    public static final String CONTAINS = "co";
    /** the "starts-with" operator. */
    public static final String STARTS_WITH = "sw";

    private QueryFilterOperators() {
        // prevent construction
    }
}
