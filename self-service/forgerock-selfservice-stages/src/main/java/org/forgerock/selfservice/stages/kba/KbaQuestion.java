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

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents a single KBA question in various Locales.
 *
 * @since 0.2.0
 */
public final class KbaQuestion {

    private String id;

    @JsonProperty ("question")
    private final Map<String, String> questions;

    /**
     * Creates a new kba question.
     */
    public KbaQuestion() {
        questions = new HashMap<>();
    }

    /**
     * Gets the unique Id of the KBA question.
     *
     * @return the unique Id
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the unique Id of the KBA question.
     *
     * @param id
     *         the unique Id
     *
     * @return this instance
     */
    public KbaQuestion setId(String id) {
        this.id = id;
        return this;
    }

    /**
     * Gets the question associated with the locale.
     *
     * @param locale
     *         the locale of the question to be returned
     *
     * @return the question
     */
    public String get(String locale) {
        return questions.get(locale);
    }

    /**
     * Associates the specified question with the specified locale.
     *
     * @param locale
     *         the locale info with which the specified question is to be associated
     *
     * @param question
     *         the question to be associated with the specified locale
     *
     * @return this instance
     */
    public KbaQuestion put(String locale, String question) {
        questions.put(locale, question);
        return this;
    }

    /**
     * Gets the unmodifiable Map view of the locales and associated questions.
     *
     * @return the locales and questions as Map
     */
    public Map<String, String> questionsAsMap() {
        return Collections.unmodifiableMap(questions);
    }

}

