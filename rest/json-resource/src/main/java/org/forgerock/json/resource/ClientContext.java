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

import org.forgerock.util.Reject;

import java.util.HashMap;
import java.util.Map;

/**
 * A Context to establish the source of a client request through the use of one
 * or more {@link Protocol} objects.
 */
public interface ClientContext extends Context {

    /**
     * A source, or "protocol" class that indicates what type of client is 
     * submitting a request.  For example, {@link HttpContext} may implement
     * <pre>
     *     public boolean hasProtocol(Protocol protocol) {
     *         Protocol.valueOf("http").equals(protocol);
     *     }
     * </pre>
     * to indicate it is the http protocol.  
     */
    public static class Protocol {
        /** the protocol value */
        private String protocol;

        /**
         * Construct a new Protocol.  Private access; callers must use {@link valueOf} to
         * instantiate a Protocol from a String.
         *
         * @param protocol the protocol value
         */
        private Protocol(final String protocol) {
            Reject.ifNull(protocol, "Protocol cannot wrap null value");
            this.protocol = protocol;
        }

        /**
         * Return this object as a String.
         *
         * @return the String representation of this object
         */
        public String toString() {
            return protocol;
        }

        /**
         * Return whether this object is the same object as the parameter.
         *
         * @param o Object for comparison
         * @return true if this object is equal to <em>o</em>, false otherwise
         */
        public boolean equals(Object o) {
            if (!(o instanceof Protocol) || o == null) {
                return false;
            }
            Protocol p = (Protocol) o;
            return p.protocol.equals(this.protocol);
        }

        /**
         * Compute the hashcode of this object.
         *
         * @return the hashcode of this object
         */
        public int hashCode() {
            return protocol.hashCode();
        }

        /**
         * Cache of created protocols.
         */
        private static Map<String,Protocol> protocols = new HashMap<String,Protocol>(0);

        /**
         * Return a Protocol object based on the parameter <em>protocol</em>.  Caches
         * existing Protocol objects to reduce unnecessary object creation.
         *
         * @param protocol the protocol value
         * @return the Protocol object
         */
        public static Protocol valueOf(final String protocol) {
            Reject.ifNull(protocol, "Protocol cannot wrap null value");
            if (!protocols.containsKey(protocol)) {
                protocols.put(protocol, new Protocol(protocol));
            }
            return protocols.get(protocol);
        }
    }

    /**
     * Return whether this Context has a particular Protocol.
     *
     * @param protocol the Protocol to check
     * @return true if the Context has the <em>protocol</em>, false otherwise
     */
    public boolean hasProtocol(Protocol protocol);

}
