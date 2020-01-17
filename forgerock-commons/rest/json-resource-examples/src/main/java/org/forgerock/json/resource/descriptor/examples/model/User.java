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
 * Copyright 2016 ForgeRock AS.
 */

package org.forgerock.json.resource.descriptor.examples.model;

import javax.validation.constraints.NotNull;
import java.util.HashSet;
import java.util.Set;

import org.forgerock.api.annotations.Description;
import org.forgerock.api.annotations.Title;
import org.forgerock.api.annotations.UniqueItems;

/**
 * User bean.
 */
@Title("User")
@Description("User with device sub-resources")
public final class User {

    @NotNull
    @Title("User ID")
    @Description("Unique user identifier")
    private final String uid;

    @NotNull
    @Title("User name")
    @Description("The user name")
    private final String name;

    @NotNull
    @Title("Password")
    @Description("Password of the user")
    private String password;

    @Title("Devices")
    @Description("Devices belonging to this user")
    @UniqueItems
    private Set<Device> devices = new HashSet<>();

    /**
     * Default User constructor.
     * @param uid user id
     * @param name user name
     * @param password user password
     */
    public User(String uid, String name, String password) {
        this.uid = uid;
        this.name = name;
        this.password = password;
    }

    /**
     * Getter of the user id.
     * @return user id
     */
    public String getUid() {
        return uid;
    }

    /**
     * Getter of the user name.
     * @return user name
     */
    public String getName() {
        return name;
    }

    /**
     * Getter of the user password.
     * @return user password
     */
    public String getPassword() {
        return password;
    }

    /**
     * Setter of the user password.
     * @param password user password
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * Getter of the Set of user devices.
     * @return Set of user devices
     */
    public Set<Device> getDevices() {
        return devices;
    }

    /**
     * Setter of the user devices.
     * @param devices Set of user devices
     */
    public void setDevices(Set<Device> devices) {
        this.devices = devices;
    }

    /**
     * Adds a device to the user devices set.
     * @param device A device
     */
    public void addDevices(Device device) {
        this.devices.add(device);
    }
}
