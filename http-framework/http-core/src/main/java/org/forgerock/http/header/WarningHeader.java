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
 * Copyright 2015-2016 ForgeRock AS.
 */

package org.forgerock.http.header;

import static org.forgerock.http.header.HeaderUtil.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import org.forgerock.http.protocol.Header;
import org.forgerock.http.protocol.Message;
import org.forgerock.util.Reject;

/**
 * Processes the {@link Warning} message header. For more information, see
 * <a href="http://www.ietf.org/rfc/rfc2616.txt">RFC 2616</a> 14.46.  This class is immutable and thread-safe.
 */
public final class WarningHeader extends Header {

    /**
     * Constructs a new header, initialized from the specified message.
     *
     * @param message The message to initialize the header from.
     * @return The parsed header.
     */
    public static WarningHeader valueOf(final Message message) {
        return new WarningHeader(toWarnings(parseMultiValuedHeader(message, NAME)));
    }

    /**
     * Constructs a new header, initialized from the specified string value.
     *
     * @param header The value to initialize the header from.
     * @return The parsed header.
     */
    public static WarningHeader valueOf(final String header) {
        return new WarningHeader(Warning.valueOf(header));
    }

    /**
     * Matches warning-headers from a {@link List} of header-values.
     *
     * @param headers Array of header values
     * @return All items in {@code headers} that are a valid warning-header
     */
    protected static List<Warning> toWarnings(final List<String> headers) {
        final List<Warning> warnings = new ArrayList<>(headers.size());
        for (final String entry : headers) {
            final Warning warning = Warning.valueOf(entry);
            if (warning != null) {
                warnings.add(warning);
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
    public static WarningHeader newWarning(final String agentName, final String fmt, final Object... args) {
        return new WarningHeader(new Warning(NOT_PRESENT, agentName, String.format(fmt, args)));
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

    /**
     * Unmodifiable {@link List} of {@link Warning}s.
     */
    private final List<Warning> warnings;

    /**
     * Constructor for single {@link Warning}.
     *
     * @param warning Single {@link Warning}
     */
    public WarningHeader(final Warning warning) {
        final List<Warning> warnings = new ArrayList<>(1);
        warnings.add(Reject.checkNotNull(warning));
        this.warnings = Collections.unmodifiableList(warnings);
    }

    /**
     * Constructor for multiple {@link Warning}s.
     *
     * @param warnings Multiple {@link Warning}s
     */
    public WarningHeader(final List<Warning> warnings) {
        this.warnings = Collections.unmodifiableList(new ArrayList<>(Reject.checkNotNull(warnings)));
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
    public WarningHeader add(final int code, final String agent, final String text) {
        return add(code, agent, text, null);
    }

    /**
     * Constructs a new header with the warnings defined in {@literal this}
     * {@code WarningHeader} in addition to the provided warning.
     *
     * @param code The warning code.
     * @param agent Name of the component responsible for issuing the warning.
     * @param text The warning text.
     * @param date The warning date or {@code null}.
     * @return A new {@code WarningHeader} instance.
     */
    public WarningHeader add(final int code, final String agent, final String text, final Date date) {
        return add(new Warning(code, agent, text, date));
    }

    /**
     * Constructs a new header with the warnings defined in {@literal this}
     * {@code WarningHeader} in addition to the provided warning.
     *
     * @param warning The warning.
     * @return A new {@code WarningHeader} instance.
     */
    public WarningHeader add(final Warning warning) {
        Reject.ifNull(warning);
        final List<Warning> list = new ArrayList<>(warnings.size() + 1);
        list.addAll(warnings);
        list.add(warning);
        return new WarningHeader(list);
    }

    /**
     * Gets all {@link Warning}s.
     *
     * @return All {@link Warning}s
     */
    public List<Warning> getWarnings() {
        return warnings;
    }

    @Override
    public List<String> getValues() {
        final List<String> headerValues = new ArrayList<>(warnings.size());
        for (final Warning warning : warnings) {
            headerValues.add(warning.toString());
        }
        return Collections.unmodifiableList(headerValues);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final WarningHeader that = (WarningHeader) o;
        return Objects.equals(warnings, that.warnings);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), warnings);
    }

    static class Factory extends HeaderFactory<WarningHeader> {

        @Override
        public WarningHeader parse(final String value) {
            return valueOf(value);
        }

        @Override
        public WarningHeader parse(final List<String> values) {
            return new WarningHeader(toWarnings(values));
        }
    }

}
