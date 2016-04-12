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
* Copyright 2014-2016 ForgeRock AS.
*/

package org.forgerock.json.resource;

import static org.forgerock.http.header.HeaderUtil.quote;

import org.forgerock.util.Reject;

/**
 * WarningHeader implements RFC 2616 section 14.46 - Warning.
 *
 * It implements Advice, which allows it to be used during the routing of CREST requests
 * such that it can be added into the response in an appropriate location.
 *
 * @see <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html">
 *     http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html</a>
 * @since 2.4.0
 */
public final class AdviceWarning {

    /**
     * 100 Indicates that there is data missing from the request.
     *
     * ForgeRock-Specific.
     */
    public final static int NOT_PRESENT = 100;

    /**
     *  110 Response is stale MUST be included whenever the returned response is stale.
     */
    public static final int RESPONSE_STALE = 110;

    /**
     *  111 Revalidation failed MUST be included if a cache returns a stale response because
     *  an attempt to revalidate the response failed, due to an inability to reach the server.
     */
    public static final int REVALIDATION_FAILED = 111;

    /**
     *  112 Disconnected operation SHOULD be included if the cache is intentionally
     *  disconnected from the rest of the network for a period of time.
     */
    public static final int DISCONNECTED_OPERATION = 112;

    /**
     *  113 Heuristic expiration MUST be included if the cache heuristically chose a
     *  freshness lifetime greater than 24 hours and the response's age is greater than 24 hours.
     */
    public static final int HEURISTIC_EXPIRATION = 113;

    /**
     *  199 Miscellaneous warning The warning text MAY include arbitrary information to be
     *  presented to a human user, or logged. A system receiving this warning MUST NOT take
     *  any automated action, besides presenting the warning to the user.
     */
    public static final int MISCELLANEOUS_WARNING = 199;

    /**
     * 214 Transformation applied MUST be added by an intermediate cache or proxy if it applies
     * any transformation changing the content-coding (as specified in the Content-Encoding header)
     * or media-type (as specified in the Content-Type header) of the response, or the entity-body
     * of the response, unless this Warning code already appears in the response.
     */
    public static final int TRANFORMATION_APPLIED = 214;

    /**
     * 299 Miscellaneous persistent warning The warning text MAY include arbitrary information to
     * be presented to a human user, or logged. A system receiving this warning MUST NOT take any automated action.
     */
    public static final int MISCELLANEOUS_PERSISTENT_WARNING = 299;

    private final int warningCode;
    private final String warningAgent;
    private final String warningText;

    private AdviceWarning(Builder builder) {
        Reject.ifNull(builder.warningAgent, builder.warningText);
        warningCode = builder.warningCode;
        warningAgent = builder.warningAgent;
        warningText = builder.warningText;
    }

    @Override
    public String toString() {
        return String.valueOf(warningCode) + " " + warningAgent + " " + quote(warningText);
    }

    /**
     * Convenience method to quickly generate frequently-used error type: 100.
     *
     * @param agentName Name of the component responsible for issuing the warning.
     * @param missingKey Name of the missing key which must be included.
     * @return a newly constructed AdviceWarning indicating the expected key was not found in the request.
     */
    public static AdviceWarning getNotPresent(String agentName, String missingKey) {
        return AdviceWarning.newBuilder()
                .withWarningAgent(agentName)
                .withWarningCode(NOT_PRESENT)
                .withWarningText(missingKey + " should be included in the request.")
                .build();
    }

    /**
     * Generate a warning using the builder provided.
     *
     * @param agentName the agent name
     * @param fmt The format, which may include embedded %s, etc.
     * @param args Zero or more args, passed into String.format to generate the warning text
     * @return a newly built WarningHeader object
     */
    public static AdviceWarning newAdviceWarning(String agentName, String fmt, Object... args) {
        return AdviceWarning
                .newBuilder()
                .withWarningAgent(agentName)
                .withWarningCode(NOT_PRESENT)
                .withWarningText(String.format(fmt, args))
                .build();
    }

    private static Builder newBuilder() {
        return new Builder();
    }

    /**
     * Accessed via {@link AdviceWarning#newBuilder()}.
     */
    private static final class Builder {

        private int warningCode;
        private String warningAgent;
        private String warningText;

        /**
         * Package private default CTOR to prevent direct instantiation by other than us.
         */
        private Builder() {
        }

        /**
         * A three-digit code which can be linked back to the cause of the Warning.
         *
         * @param warningCode a three-digit integer.
         * @return this builder.
         */
        private Builder withWarningCode(int warningCode) {
            Reject.ifTrue(warningCode < 0);
            Reject.ifTrue(String.valueOf(warningCode).length() != 3);
            this.warningCode = warningCode;
            return this;
        }

        /**
         * An identifier, used for debugging so that the receiving agent can
         * determine from where this Warning originated.
         *
         * @param warningAgent a String identifier.
         * @return this builder.
         */
        private Builder withWarningAgent(String warningAgent) {
            Reject.ifNull(warningAgent);
            Reject.ifTrue(warningAgent.isEmpty());
            this.warningAgent = warningAgent;
            return this;
        }

        /**
         * A human-readable description of the Warning.
         *
         * @param warningText a String description
         * @return this builder.
         */
        private Builder withWarningText(String warningText) {
            Reject.ifNull(warningText);
            Reject.ifTrue(warningText.isEmpty());
            this.warningText = warningText;
            return this;
        }

        /**
         * Builds and returns a valid and usable {@code WarningHeader} from this
         * builder.
         *
         * @return The built {@code WarningHeader}.
         */
        private AdviceWarning build() {
            return new AdviceWarning(this);
        }
    }
}
