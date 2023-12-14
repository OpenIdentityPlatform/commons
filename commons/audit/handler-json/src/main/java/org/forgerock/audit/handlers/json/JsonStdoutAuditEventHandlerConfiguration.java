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
 * Copyright 2023 3A Systems LLC
 */

package org.forgerock.audit.handlers.json;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import org.forgerock.audit.events.handlers.EventHandlerConfiguration;

/**
 * Configuration for {@link JsonAuditEventHandler}.
 */
public class JsonStdoutAuditEventHandlerConfiguration extends EventHandlerConfiguration {

    @JsonPropertyDescription("audit.handlers.json.elasticsearchCompatible")
    private boolean elasticsearchCompatible;

    /**
     * Determines if JSON format should be transformed to be compatible with ElasticSearch format restrictions.
     *
     * @return {@code true} for ElasticSearch JSON format compatibility enforcement and {@code false} otherwise
     */
    public boolean isElasticsearchCompatible() {
        return elasticsearchCompatible;
    }

    /**
     * Specifies if JSON format should be transformed to be compatible with ElasticSearch format restrictions.
     *
     * @param elasticsearchCompatible {@code true} for ElasticSearch JSON format compatibility enforcements and
     * {@code false} otherwise
     */
    public void setElasticsearchCompatible(boolean elasticsearchCompatible) {
        this.elasticsearchCompatible = elasticsearchCompatible;
    }

    @Override
    public boolean isUsableForQueries() {
        return false;
    }
}
