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

package org.forgerock.selfservice.core;

import static org.forgerock.selfservice.core.ServiceUtils.emptyJson;

import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.selfservice.core.snapshot.SnapshotTokenHandler;

/**
 * Indicates whether the service should operate in stateless or stateful mode.
 * <br />
 * Stateless means that all process state will be pushed into the token that
 * is returned to the client. Whereas stateful (local) will push all state to
 * a local store and the returning token will be used to key that state.
 *
 * @since 0.1.0
 */
public enum StorageType {

    /**
     * State should be preserved locally.
     */
    LOCAL(new SnapshotAuthorFactory() {

        @Override
        public SnapshotAuthor newSnapshotAuthor(SnapshotTokenHandler tokenHandler, ProcessStore store) {
            return new LocalSnapshotAuthor(tokenHandler, store);
        }

    }),

    /**
     * State is serialised into request/response to avoid server-side state management.
     */
    STATELESS(new SnapshotAuthorFactory() {

        @Override
        public SnapshotAuthor newSnapshotAuthor(SnapshotTokenHandler tokenHandler, ProcessStore store) {
            return new StatelessSnapshotAuthor(tokenHandler);
        }

    });

    private final SnapshotAuthorFactory factory;

    StorageType(SnapshotAuthorFactory factory) {
        this.factory = factory;
    }

    SnapshotAuthor newSnapshotAuthor(SnapshotTokenHandler tokenHandler, ProcessStore store) {
        return factory.newSnapshotAuthor(tokenHandler, store);
    }

    /*
     * Factory used to create new snapshot authors.
     */
    private interface SnapshotAuthorFactory {

        SnapshotAuthor newSnapshotAuthor(SnapshotTokenHandler tokenHandler, ProcessStore store);

    }

    /*
     * The local snapshot author is for preserving state local to the server
     * in some store. In this case the snapshot token acts as a key to persisted
     * entry. This results in the service being stateful.
     */
    private static final class LocalSnapshotAuthor implements SnapshotAuthor {

        private final SnapshotTokenHandler handler;
        private final ProcessStore store;

        LocalSnapshotAuthor(SnapshotTokenHandler handler, ProcessStore store) {
            this.handler = handler;
            this.store = store;
        }

        @Override
        public String captureSnapshotOf(JsonValue state) throws ResourceException {
            String snapshotToken = handler.generate(emptyJson());
            store.add(snapshotToken, state);
            return snapshotToken;
        }

        @Override
        public JsonValue retrieveSnapshotFrom(String snapshotToken) throws ResourceException {
            handler.validate(snapshotToken);
            return store.remove(snapshotToken);
        }

    }

    /*
     * The stateless snapshot author is for preserving state within the
     * snapshot token itself, therefore never needing to store state local
     * to the server. It is stateless in that state is not stored locally.
     */
    private static final class StatelessSnapshotAuthor implements SnapshotAuthor {

        private final SnapshotTokenHandler handler;

        StatelessSnapshotAuthor(SnapshotTokenHandler handler) {
            this.handler = handler;
        }

        @Override
        public String captureSnapshotOf(JsonValue state) throws ResourceException {
            return handler.generate(state);
        }

        @Override
        public JsonValue retrieveSnapshotFrom(String snapshotToken) throws ResourceException {
            return handler.validateAndExtractState(snapshotToken);
        }

    }

}
