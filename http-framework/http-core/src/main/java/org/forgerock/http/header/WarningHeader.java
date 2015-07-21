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

package org.forgerock.http.header;

import static org.forgerock.http.header.HeaderUtil.parseMultiValuedHeader;

import java.util.ArrayList;
import java.util.List;

import org.forgerock.http.protocol.Header;
import org.forgerock.http.protocol.Message;
import org.forgerock.util.Reject;

/**
 * Processes the <strong>{@code Warning}</strong> message header. For more
 * information, see <a href="http://www.ietf.org/rfc/rfc2616.txt">RFC 2616</a>
 * 14.46.
 */
public final class WarningHeader implements Header {

    /**
     * Constructs a new header, initialized from the specified message.
     *
     * @param message The message to initialize the header from.
     * @return The parsed header.
     */
    public static WarningHeader valueOf(Message message) {
        return new WarningHeader(toWarnings(parseMultiValuedHeader(message, NAME)));
    }

    /**
     * Constructs a new header, initialized from the specified string value.
     *
     * @param string The value to initialize the header from.
     * @return The parsed header.
     */
    public static WarningHeader valueOf(String string) {
        return new WarningHeader(toWarnings(parseMultiValuedHeader(string)));
    }

    private static List<Warning> toWarnings(List<String> header) {
        List<Warning> warnings = new ArrayList<>();
        for (String entry : header) {
            String[] parts = entry.split(" ");
            if (parts.length >= 3) {
                warnings.add(new Warning(Integer.parseInt(parts[0]), parts[1], parts[2]));
            }
        }
        return warnings;
    }

    /**
     * Constructs a new warning header with the frequently-used error type:
     * 100.
     *
     * @param agentName Name of the component responsible for issuing the
     *                  warning.
     * @param fmt The format, which may include embedded %s, etc.
     * @param args Zero or more args, passed into String.format to generate the
     *             warning text.
     * @return A newly constructed {@code WarningHeader} indicating the
     * expected key was not found in the request.
     */
    public static WarningHeader newWarning(String agentName, String fmt, Object... args) {
        return new WarningHeader(NOT_PRESENT, agentName, String.format(fmt, args));
    }

    /** The name of this header. */
    public static final String NAME = "Warning";

    /**
     * 100 Indicates that there is data missing from the request.
     *
     * <p>ForgeRock-Specific.</p>
     */
    public final static int NOT_PRESENT = 100;

    /**
     * 110 Response is stale MUST be included whenever the returned response
     * is stale.
     */
    public static final int RESPONSE_STALE = 110;

    /**
     * 111 Revalidation failed MUST be included if a cache returns a stale
     * response because an attempt to revalidate the response failed, due to an
     * inability to reach the server.
     */
    public static final int REVALIDATION_FAILED = 111;

    /**
     * 112 Disconnected operation SHOULD be included if the cache is
     * intentionally disconnected from the rest of the network for a period of
     * time.
     */
    public static final int DISCONNECTED_OPERATION = 112;

    /**
     * 113 Heuristic expiration MUST be included if the cache heuristically
     * chose a freshness lifetime greater than 24 hours and the response's age
     * is greater than 24 hours.
     */
    public static final int HEURISTIC_EXPIRATION = 113;

    /**
     * 199 Miscellaneous warning The warning text MAY include arbitrary
     * information to be presented to a human user, or logged. A system
     * receiving this warning MUST NOT take any automated action, besides
     * presenting the warning to the user.
     */
    public static final int MISCELLANEOUS_WARNING = 199;

    /**
     * 214 Transformation applied MUST be added by an intermediate cache or
     * proxy if it applies any transformation changing the content-coding (as
     * specified in the Content-Encoding header) or media-type (as specified in
     * the Content-Type header) of the response, or the entity-body of the
     * response, unless this Warning code already appears in the response.
     */
    public static final int TRANFORMATION_APPLIED = 214;

    /**
     * 299 Miscellaneous persistent warning The warning text MAY include
     * arbitrary information to be presented to a human user, or logged. A
     * system receiving this warning MUST NOT take any automated action.
     */
    public static final int MISCELLANEOUS_PERSISTENT_WARNING = 299;

    private final List<Warning> warnings = new ArrayList<>();

    private static final class Warning {
        private final int code;
        private final String agent;
        private final String text;

        private Warning(int code, String agent, String text) {
            this.code = code;
            this.agent = agent;
            this.text = text;
        }

        @Override
        public String toString() {
            return String.valueOf(code) + " " + agent + " " + text;
        }
    }

    private WarningHeader(List<Warning> warnings) {
        this.warnings.addAll(warnings);
    }

    /**
     * Constructs a new header with the provided warning.
     *
     * @param code The warning code.
     * @param agent Name of the component responsible for issuing the warning.
     * @param text The warning text.
     */
    public WarningHeader(int code, String agent, String text) {
        Reject.ifNull(agent, text);
        warnings.add(new Warning(code, agent, text));
    }

    @Override
    public String getName() {
        return NAME;
    }

    /**
     * Constructs a new header with the warnings defined in {@literal this}
     * {@code WarningHeader} in addition to the provided warning.
     *
     * @param code The warning code.
     * @param agent Name of the component responsible for issuing the warning.
     * @param text The warning text.
     * @return A new {@code WarningHeader} instance.
     */
    public WarningHeader add(int code, String agent, String text) {
        WarningHeader header = new WarningHeader(warnings);
        header.warnings.add(new Warning(code, agent, text));
        return header;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Warning warning : warnings) {
            sb.append(warning.toString()).append("; ");
        }
        return sb.toString().trim();
    }
}
