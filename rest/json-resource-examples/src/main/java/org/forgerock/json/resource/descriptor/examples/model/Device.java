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

import java.util.Date;

/**
 * Bean type class for Device object.
 */
public class Device {

    private final String did;
    private final String name;
    private final String type;
    private boolean stolen;
    private Date rollOutDate;

    /**
     * Default constructor.
     * @param did Device id
     * @param name Device Name
     * @param type Device Type
     */
    public Device(String did, String name, String type) {
        this.did = did;
        this.name = name;
        this.type = type;
    }

    /**
     * Getter of the device id.
     * @return Device id
     */
    public String getDid() {
        return did;
    }

    /**
     * Getter of the device name.
     * @return Device name
     */
    public String getName() {
        return name;
    }

    /**
     * Getter of the device type.
     * @return Device type
     */
    public String getType() {
        return type;
    }

    /**
     * True is the device has been flagged as stolen.
     * @return true if device is stolen
     */
    public boolean isStolen() {
        return stolen;
    }

    /**
     * Setter of the device stolen flag.
     * @param stolen True is the device is stolen
     */
    public void setStolen(boolean stolen) {
        this.stolen = stolen;
    }

    /**
     * Getter of the device rollout date.
     * @return Device rollout date
     */
    public Date getRollOutDate() {
        return rollOutDate;
    }

    /**
     * Setter of the device rollout date.
     * @param rollOutDate Device rollOut date
     */
    public void setRollOutDate(Date rollOutDate) {
        this.rollOutDate = rollOutDate;
    }
}
