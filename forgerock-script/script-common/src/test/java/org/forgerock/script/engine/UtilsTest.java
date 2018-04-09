/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 ForgeRock AS. All Rights Reserved
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

package org.forgerock.script.engine;

import org.fest.assertions.data.MapEntry;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.testng.Assert.assertTrue;

/**
 * A NAME does ...
 *
 * @author Laszlo Hordos
 */
public class UtilsTest {
    @Test
    public void testDeepCopy() throws Exception {

        HashMap<String, Object> paper = new HashMap<String, Object>();
        // JsonValue scissors = new JsonValue(new HashMap<String,Object>());
        Map<String, Object> scissors = new HashMap<String, Object>();
        List<Object> rock = new ArrayList<Object>();

        // Paper
        paper.put("rock", "crushes");
        paper.put("covers", rock);

        // Rock
        rock.add("crushes");
        rock.add(scissors);
        rock.add(paper);

        // Scissors
        scissors.put("scissors", "crushes");
        scissors.put("cuts", paper);
        scissors.put("paper", paper);

        Object copyOfPaper = Utils.deepCopy(paper);
        assertTrue(copyOfPaper instanceof Map);
        assertThat((Map) copyOfPaper).contains(MapEntry.entry("rock", "crushes"))
                .isNotSameAs(paper);

        Object copyOfRock = Utils.deepCopy(rock);

        Object copyOfScissors = Utils.deepCopy(scissors);

    }
}
