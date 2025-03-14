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
    "jquery",
    "lodash",
    "sinon",
    "org/forgerock/commons/ui/common/main/ValidatorsManager"
], function ($, _, sinon, ValidatorsManager) {
    var container = $("<div>")
        .append("<input id='test' data-validator='testValidatorMethod' data-validation-dependents='dependent' data-validator-event='custom'>")
        .append("<input id='dependent' data-validator='testValidatorMethod'>");

    QUnit.module('ValidatorsManager Functions');

    ValidatorsManager.updateConfigurationCallback({
        "validators": {
            "testValidatorMethod": {
                "dependencies": [ ],
                "validator": function(el, input, callback) {
                    var v = input.val();
                    if (v === "GOOD") {
                        callback();
                    } else {
                        callback(["DOES NOT PASS"]);
                    }
                }
            }
        }
    });

    QUnit.test("bindValidators", function (assert) {
        var callbackFunction = sinon.spy(),
            extraAfterValidatorsFunction = sinon.spy();

        sinon.stub(ValidatorsManager, "bindValidatorsForField");

        ValidatorsManager.afterBindValidators.push(extraAfterValidatorsFunction);

        ValidatorsManager.bindValidators(container, callbackFunction);

        assert.equal(ValidatorsManager.bindValidatorsForField.callCount, container.find(":input").length,
            "bindValidatorsForField called once for each element in provided container");

        assert.ok(callbackFunction.calledOnce, "callback function provided to bindValidators invoked once");

        assert.ok(extraAfterValidatorsFunction.calledOnce && extraAfterValidatorsFunction.calledWithExactly(container, callbackFunction),
            "function injected into afterBindValidators called once (with expected arguments) after bindValidators");

        // remove the spy we pushed onto the end
        ValidatorsManager.afterBindValidators.splice(-1);

        // don't stub the internal method after test is complete
        ValidatorsManager.bindValidatorsForField.restore();
    });

    QUnit.test("bindValidatorsForField", function (assert) {
        var field = container.find("#test"),
            eventsList;

        ValidatorsManager.bindValidatorsForField(container, field);

        eventsList = _.sortBy(_.keys($._data(field[0]).events));

        assert.ok(_.isEqual(eventsList, ["blur", "change", "custom", "keyup", "paste", "validate"]),
            "custom and default events all bound to specified input field");
    });

    QUnit.test("evaluateValidator", function (assert) {
        var done = assert.async();
        var field = container.find("#test");

        field.val("");
        ValidatorsManager.evaluateValidator("testValidatorMethod", field, container)
            .then(function (failures) {
                assert.ok(failures.length === 1 && failures[0] === "DOES NOT PASS");
            })
            .then(function () {
                field.val("GOOD");
                return ValidatorsManager.evaluateValidator("testValidatorMethod", field, container);
            })
            .then(function (failures) {
                assert.ok(!failures);
                done();
            });
    });

    QUnit.test("evaluateDependentFields", function (assert) {
        var done = assert.async();
        var primary = container.find("#test"),
            dependent = container.find("#dependent"),
            failureMessages = [];

        primary.val("GOOD");
        dependent.val("BAD");

        container
            .on("validationSuccessful", function () {
                failureMessages = [];
            })
            .on("validationFailed", function (event, data) {
                failureMessages = data.failures;
            });

        ValidatorsManager.evaluateDependentFields(primary, container)
            .then(function () {
                assert.equal(dependent.attr("data-validation-status"), "error");
                assert.ok(failureMessages.length === 1 && failureMessages[0] === "DOES NOT PASS");
            })
            .then(function () {
                dependent.val("GOOD");
                return ValidatorsManager.evaluateDependentFields(primary, container);
            })
            .then(function () {
                assert.equal(dependent.attr("data-validation-status"), "ok");
                assert.equal(failureMessages.length, 0);
                done();
            });
    });


    QUnit.test("evaluateAllValidatorsForField", function (assert) {
        var done = assert.async();
        var primary = container.find("#test"),
            dependent = container.find("#dependent"),
            failureMessages = [];

        primary.val("BAD");
        dependent.val("BAD");

        container
            .on("validationSuccessful", function () {
                failureMessages = [];
            })
            .on("validationFailed", function (event, data) {
                failureMessages = data.failures;
            });

        ValidatorsManager.evaluateAllValidatorsForField(primary, container)
            .then(function () {
                assert.equal(primary.attr("data-validation-status"), "error");
                assert.equal(dependent.attr("data-validation-status"), "error");
                assert.ok(failureMessages.length === 1 && failureMessages[0] === "DOES NOT PASS");
            })
            .then(function () {
                primary.val("GOOD");
                return ValidatorsManager.evaluateAllValidatorsForField(primary, container);
            })
            .then(function () {
                assert.equal(primary.attr("data-validation-status"), "ok");
                assert.equal(dependent.attr("data-validation-status"), "error");
                assert.ok(failureMessages.length === 1 && failureMessages[0] === "DOES NOT PASS");
            })
            .then(function () {
                dependent.val("GOOD");
                return ValidatorsManager.evaluateAllValidatorsForField(primary, container);
            })
            .then(function () {
                assert.equal(primary.attr("data-validation-status"), "ok");
                assert.equal(dependent.attr("data-validation-status"), "ok");
                assert.equal(failureMessages.length, 0);
                done();
            });
    });


});
