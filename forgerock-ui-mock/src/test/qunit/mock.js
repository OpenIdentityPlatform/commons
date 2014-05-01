define(function () {
    return {
        executeAll: function (server) {
            module('');

            var view = require('RegisterView');

            QUnit.asyncTest('User Registration', function () {
                view.render(null, function () {

                    var pwd = $('input[name="password"]'),
                        pwdConfirm = $('input[name="passwordConfirm"]');

                    start();
                    equal($('input[name="register"]').attr('disabled'), 'disabled', 'Initial state of submit button is not disabled');

                    equal($('input[name="userName"]').data('validation-status'), 'error', "Empty username field passes validation");
                    equal($('input[name="mail"]').data('validation-status'), 'error', "Empty e-mail field passes validation");
                    equal($('input[name="givenName"]').data('validation-status'), 'error', "Empty first name field passes validation");
                    equal($('input[name="sn"]').data('validation-status'), 'error', "Empty last name field passes validation");
                    equal($('input[name="telephoneNumber"]').data('validation-status'), 'error', "Empty mobile phone field passes validation");
                    equal($('input[name="password"]').data('validation-status'), 'error', "Empty password field passes validation");
                    equal($('input[name="passwordConfirm"]').data('validation-status'), 'error', "Empty password confirm field passes validation");

                    equal($('input[name="terms"]').data('validation-status'), 'error', "Initial state of terms checkbox passes validation");

                    pwd.val('abc').trigger('change');
                    equal($('input[name="password"]').attr('data-validation-status'), 'error', 'Simplest password rejected');

                    pwd.val('abc1').trigger('change');
                    ok($("div.validationRules[data-for-validator='password passwordConfirm'] div.field-rule span[data-for-req='AT_LEAST_X_NUMBERS']").parent().find("span.error").length === 0, 'Number added to password satisfied AT_LEAST_X_NUMBERS');

                    pwd.val('Passw0rd').trigger('change');
                    pwdConfirm.val('Passw0rd').trigger('change');

                    equal($('input[name="password"]').attr('data-validation-status'), 'ok', 'Password passes validation');
                    equal($('input[name="passwordConfirm"]').attr('data-validation-status'), 'ok', 'Password confirm field passes validation');

                });
            });

/*
            asyncTest('Valid User', function () {

            });

            asyncTest('E-mail validation', function () {

            });

            asyncTest('Mobile phone validation', function () {

            });

            asyncTest('Submitting correct data', function () {

            });
*/
        }
    }
});