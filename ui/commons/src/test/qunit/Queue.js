/**
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

define([
    "org/forgerock/commons/ui/common/util/Queue"
], function (Queue) {
    QUnit.module('Queue Functions');

    QUnit.test("core operations", function (assert) {
        var q = new Queue(["a","b"]);

        assert.equal(q.peek(), "a");
        assert.equal(q.remove(), "a");
        assert.equal(q.remove(), "b");
        q.add("c");
        assert.equal(q.remove(), "c");
        assert.equal(q.peek(), undefined);
        assert.equal(q.remove(), undefined);

    });

});
