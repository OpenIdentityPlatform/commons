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
* Copyright 2014-2016 ForgeRock AS.
*/

package org.forgerock.json.resource;

import static org.forgerock.json.JsonValueFunctions.setOf;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;

import org.forgerock.services.context.AbstractContext;
import org.forgerock.services.context.Context;
import org.forgerock.json.JsonValue;
import org.forgerock.util.Reject;

/**
 * A {@link Context} containing information which should be returned to the user in some
 * appropriate form to the user. For example, it could be contained within the body of the response
 * or otherwise added to the headers returned.
 *
 * @since 2.4.0
 */
public class AdviceContext extends AbstractContext {

    /** the persisted attribute name for the advices. */
    private static final String ADVICE_ATTR = "advice";

    /** The persisted attribute name for the restricted advice names. */
    private static final String RESTRICTED_ADVICE_NAMES_ATTR = "restrictedAdviceNames";

    private static final Pattern ALLOWED_RFC_CHARACTERS = Pattern.compile("^[\\x20-\\x7E]*$");

    private final Collection<String> restrictedAdviceNames = new HashSet<>();

    /** Advice currently stored for this context is help in this map. **/
    private final Map<String, List<String>> advice = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

    /**
     * Creates a new AdviceContext with the provided parent.
     *
     * @param parent The parent context.
     * @param restrictedAdviceNames The restricted advice names.
     */
    public AdviceContext(Context parent, Collection<String> restrictedAdviceNames) {
        super(parent, "advice");
        this.restrictedAdviceNames.addAll(restrictedAdviceNames);
        data.put(RESTRICTED_ADVICE_NAMES_ATTR, restrictedAdviceNames);
        data.put(ADVICE_ATTR, advice);
    }

    /**
     * Restore from JSON representation.
     *
     * @param savedContext
     *            The JSON representation from which this context's attributes
     *            should be parsed.
     * @param classLoader
     *            The ClassLoader which can properly resolve the persisted class-name.
     */
    public AdviceContext(final JsonValue savedContext, final ClassLoader classLoader) {
        super(savedContext, classLoader);
        restrictedAdviceNames.addAll(data.get(RESTRICTED_ADVICE_NAMES_ATTR).as(setOf(String.class)));
        advice.putAll(data.get(ADVICE_ATTR).asMapOfList(String.class));
    }

    /**
     * Returns the advices contained within this context.
     *
     * @return the advices contained within this context.
     */
    public Map<String, List<String>> getAdvices() {
        return advice;
    }

    /**
     * Adds advice to the context, which can be retrieved and later returned to the user.
     *
     * @param adviceName Name of the advice to return to the user. Not null.
     * @param advices Human-readable advice to return to the user. Not null.
     */
    public void putAdvice(String adviceName, String... advices) {
        Reject.ifNull(adviceName, advices);
        Reject.ifTrue(isRestrictedAdvice(adviceName), "Illegal use of restricted advice name, " + adviceName);
        for (String adviceEntry : advices) {
            Reject.ifTrue(!isRfcCompliant(adviceEntry), "Advice contains illegal characters in, " + adviceEntry);
        }
        List<String> adviceEntry = advice.get(adviceName);
        if (adviceEntry == null) {
            adviceEntry = new ArrayList<>();
            advice.put(adviceName, adviceEntry);
        }
        adviceEntry.addAll(Arrays.asList(advices));
    }

    private boolean isRfcCompliant(String advice) {
        return ALLOWED_RFC_CHARACTERS.matcher(advice).matches();
    }

    private boolean isRestrictedAdvice(String adviceName) {
        return restrictedAdviceNames.contains(adviceName);
    }
}
