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

import java.util.HashMap;
import java.util.Map;

import org.forgerock.resource.core.AbstractContext;
import org.forgerock.resource.core.Context;
import org.forgerock.util.Reject;

/**
 * A {@link Context} containing information which should be returned to the user in some
 * appropriate form to the user. For example, it could be contained within the body of the response
 * or otherwise added to the headers returned.
 *
 * @since 2.4.0
 */
public class AdviceContext extends AbstractContext {

    /** advice currently stored for this context is help in this map. */
    private final Map<String, String> advice = new HashMap<String, String>();

    /**
     * Creates a new AdviceContext with the provided parent
     * 
     * @param parent
     *            The parent context.
     */
    public AdviceContext(Context parent) {
        super(parent, "advice");
    }

    /**
     * Returns the advices contained within this context.
     * 
     * @return the advices contained within this context.
     */
    public Map<String, String> getAdvices() {
        return advice;
    }

    /**
     * Adds a piece of advice to the context, which can be retrieved and later returned to the user.
     *
     * @param adviceName Name of the advice to return to the user. Not null.
     * @param advice Human-readable advice to return to the user. Not null.
     */
    public void putAdvice(String adviceName, String advice) {
        Reject.ifNull(adviceName, advice);
        this.advice.put(adviceName, advice);
    }
}
