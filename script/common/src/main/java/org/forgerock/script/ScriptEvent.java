/*
 * DO NOT REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 ForgeRock AS. All rights reserved.
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 */

package org.forgerock.script;

import javax.script.ScriptException;
import java.util.EventObject;

/**
 * A ScriptEvent does ...
 *
 * @author Laszlo Hordos
 */
public class ScriptEvent extends EventObject {

    private static final long serialVersionUID = 6253960209569884135L;

    /**
     * Reference to the script that had a change occur in its lifecycle.
     */
    private final transient ScriptRegistry reference;

    private final transient ScriptName name;

    /**
     * Type of script lifecycle change.
     */
    private final int type;

    /**
     * This script has been registered.
     * <p>
     * This event is synchronously delivered <strong>after</strong> the script
     * has been registered with the Library.
     *
     */
    public final static int REGISTERED = 0x00000001;

    /**
     * The properties of a registered script have been modified.
     * <p>
     * This event is synchronously delivered <strong>after</strong> the script
     * properties have been modified.
     *
     */
    public final static int MODIFIED = 0x00000002;

    /**
     * This script is in the process of being unregistered.
     * <p>
     * This event is synchronously delivered <strong>before</strong> the script
     * has completed unregistering.
     *
     * <p>
     * If a bundle is using a script that is <code>UNREGISTERING</code>, the
     * bundle should release its use of the script when it receives this event.
     * If the bundle does not release its use of the script when it receives
     * this event, the Library will automatically release the bundle's use of
     * the script while completing the script unregistration operation.
     *
     */
    public final static int UNREGISTERING = 0x00000004;

    /**
     * Creates a new script event object.
     *
     * @param type
     *            The event type.
     * @param reference
     *            A <code>ScriptLibraryEntry</code> object to the script that
     *            had a lifecycle change.
     * @param name
     *            The name of the script
     */
    public ScriptEvent(int type, ScriptRegistry reference, ScriptName name) {
        super(name);
        this.reference = reference;
        this.name = name;
        this.type = type;
    }

    /**
     * Returns a reference to the script that had a change occur in its
     * lifecycle.
     * <p>
     * This reference is the source of the event.
     *
     * @return Reference to the script that had a lifecycle change.
     */
    public ScriptEntry getScriptLibraryEntry() throws ScriptException {
        return reference.takeScript(name);
    }

    /**
     * Returns the type of event. The event type values are:
     * <ul>
     * <li>{@link #REGISTERED}</li>
     * <li>{@link #MODIFIED}</li>
     * <li>{@link #UNREGISTERING}</li>
     * </ul>
     *
     * @return Type of script lifecycle change.
     */

    public int getType() {
        return type;
    }
}
