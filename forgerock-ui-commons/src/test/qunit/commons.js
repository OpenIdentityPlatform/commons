define(function () {
    return {
        executeAll: function (server) {

            module("Common Tests");

            QUnit.test("Startup Title", function () {
                QUnit.ok($("title").text().match(/Login$/), "The initial title should say Login");
            });
        }
    };
});