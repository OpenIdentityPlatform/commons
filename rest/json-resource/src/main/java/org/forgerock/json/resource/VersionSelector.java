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
 * Copyright 2014 ForgeRock AS.
 */

package org.forgerock.json.resource;

import java.util.Collections;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * <p>Selects the best candidate based on a requested version.</p>
 *
 * <p>Visibility set to public to allow for other frameworks to make use of it to support resource versioning, i.e.
 * Restlet REST services in OpenAM.</p>
 *
 * @since 2.4.0
 */
public class VersionSelector {

    private DefaultVersionBehaviour defaultBehaviour = DefaultVersionBehaviour.LATEST;

    /**
     * Constructs a new VersionSelector instance with the default versioning behaviour of selecting the latest resource
     * version when the requested version is not specified.
     */
    public VersionSelector() {
        // Nothing to do.
    }

    /**
     * <p>Selects the best match from the given candidates for the requested version.</p>
     *
     * <p>See {@link Version#isCompatibleWith(Version)} for information on the matching logic.</p>
     *
     * <p>If the requested version is {@code null} the the default behaviour is to return the latest candidate. This can
     * be changed by calling either {@link #defaultToLatest()} or {@link #defaultToOldest()}.</p>
     *
     * @param requested The requested version.
     * @param candidates A {@code Map} of candidates.
     * @param <T> The candidate value type.
     * @return The candidate that matched the requested version.
     * @throws NotFoundException If the requested version does not match and candidate.
     */
    public <T> T select(Version requested, Map<Version, T> candidates) throws NotFoundException {

        if (candidates == null || candidates.isEmpty()) {
            //TODO i18n
            throw new NotFoundException("No match found. No routes registered.");
        }

        SortedMap<Version, T> sortedCandidates = sort(candidates);

        if (requested == null) {
            switch (defaultBehaviour) {
                case LATEST: {
                    return candidates.get(sortedCandidates.firstKey());
                }
                case OLDEST: {
                    return candidates.get(sortedCandidates.lastKey());
                }
                default: {
                    //TODO i18n
                    throw new NotFoundException("No resource version specified.");
                }
            }
        }

        for (Map.Entry<Version, T> candidate : sortedCandidates.entrySet()) {
            if (candidate.getKey().isCompatibleWith(requested)) {
                return candidate.getValue();
            }
        }

        //TODO i18n
        throw new NotFoundException("Requested version, " + requested + ", does not match any routes.");
    }

    /**
     * Sets the behaviour of the selection process to always use the latest resource version when the requested version
     * is {@code null}.
     */
    public void defaultToLatest() {
        this.defaultBehaviour = DefaultVersionBehaviour.LATEST;
    }

    /**
     * Sets the behaviour of the selection process to always use the oldest resource version when the requested version
     * is {@code null}.
     */
    public void defaultToOldest() {
        this.defaultBehaviour = DefaultVersionBehaviour.OLDEST;
    }

    /**
     * Removes the default behaviour of the selection process which will result in {@code NotFoundException}s when the
     * requested version is {@code null}.
     */
    public void noDefault() {
        this.defaultBehaviour = DefaultVersionBehaviour.NONE;
    }

    /**
     * Sorts the given map based on the version key.
     *
     * @param map The map to sort.
     * @param <T> The value type.
     * @return A {@code SortedMap}.
     */
    private <T> SortedMap<Version, T> sort(Map<Version, T> map) {
        SortedMap<Version, T> sortedMap = new TreeMap<Version, T>(Collections.reverseOrder());
        sortedMap.putAll(map);
        return sortedMap;
    }

    /**
     * Enum for describing the default behaviour when no resource version is requested.
     */
    enum DefaultVersionBehaviour {
        LATEST,
        OLDEST,
        NONE
    }
}
