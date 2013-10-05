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
 * Copyright 2013 ForgeRock AS.
 */
package org.forgerock.json.resource.api;

import java.util.Locale;

@SuppressWarnings("javadoc")
public final class Urn implements Comparable<Urn> {
    // FIXME: not sure if we want to restrict the URN format this much.

    public enum Type {
        API("api"), RESOURCE("resource");
        private final String lowerName;

        private Type(final String lowerName) {
            this.lowerName = lowerName;
        }
    }

    private final String organization;
    private final String namespace;
    private final Type type;
    private final String name;
    private final Version version;
    private final String urn;
    private final String nurn;

    private Urn(final String urn, final String organization, final String namespace,
            final Type type, final String name, final Version version) {
        this.urn = urn;
        this.organization = organization;
        this.namespace = namespace;
        this.type = type;
        this.name = name;
        this.version = version;

        final StringBuilder builder = new StringBuilder("urn:");
        builder.append(organization.toLowerCase(Locale.ENGLISH));
        builder.append(':');
        builder.append(namespace.toLowerCase(Locale.ENGLISH));
        builder.append(':');
        builder.append(type.lowerName);
        builder.append(':');
        builder.append(name.toLowerCase(Locale.ENGLISH));
        builder.append(':');
        builder.append(version); // Adds missing minor/micro versions.
        nurn = builder.toString();
    }

    /**
     * Parses the string argument as a descriptor URN. The URN must be of the
     * following form:
     * 
     * <pre>
     * urn:&lt;organization>:&lt;namespace>:&lt;type>:&lt;name>:&lt;version>
     * </pre>
     * 
     * Where {@code type} is one of "api" or "resource". Here are some examples
     * of valid descriptor URNs:
     * 
     * <pre>
     * urn:forgerock:rest:resource:user:1.0
     * urn:forgerock:openam:resource:realm:1.0
     * urn:forgerock:rest:api:repo:1.0
     * </pre>
     * 
     * @param s
     *            The string to be parsed as a descriptor URN.
     * @return The parsed descriptor URN.
     * @throws IllegalArgumentException
     *             If the string does not contain a parsable descriptor URN.
     */
    public static Urn valueOf(final String s) throws IllegalArgumentException {
        final String[] fields = s.split(":");
        try {
            if (fields.length == 6 && fields[0].equalsIgnoreCase("urn")) {
                final String organization = fields[1];
                final String namespace = fields[2];
                final Type type = Type.valueOf(fields[3].toUpperCase(Locale.ENGLISH));
                final String name = fields[4];
                final Version version = Version.valueOf(fields[5]);
                return new Urn(s, organization, namespace, type, name, version);
            }
        } catch (final IllegalArgumentException e) {
            // Invalid type or version: fall-through to generic error.
        }
        throw new IllegalArgumentException("Descriptor urn string '" + s
                + "' is not of the form 'urn:<org>:<namespace>:api|resource:<name>:<version>'");
    }

    /**
     * Returns the organization name.
     * 
     * @return The organization name.
     */
    public String getOrganization() {
        return organization;
    }

    /**
     * Returns the namespace name.
     * 
     * @return The namespace name.
     */
    public String getNamespace() {
        return namespace;
    }

    /**
     * Returns the type.
     * 
     * @return The type.
     */
    public Type getType() {
        return type;
    }

    /**
     * Returns the name.
     * 
     * @return The name.
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the version.
     * 
     * @return The version.
     */
    public Version getVersion() {
        return version;
    }

    @Override
    public int hashCode() {
        return nurn.hashCode();
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        } else if (obj instanceof Urn) {
            final Urn that = (Urn) obj;
            return nurn.equals(that.nurn);
        } else {
            return false;
        }
    }

    @Override
    public int compareTo(final Urn that) {
        return nurn.compareTo(that.nurn);
    }

    @Override
    public String toString() {
        return urn;
    }
}
