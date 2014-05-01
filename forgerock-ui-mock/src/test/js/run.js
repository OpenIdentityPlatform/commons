require([
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/mock/ui/common/main/MockServer",
    "../test/tests/commons",
    "../test/tests/user",
    "../test/tests/mock"
], function (constants, eventManager, mockServer, commonsTests, userTests, mockTests) {

    eventManager.registerListener(constants.EVENT_APP_INTIALIZED, function () {
    
        $.doTimeout = function (name, time, func) {
            func(); // run the function immediately rather than delayed.
        }

        require("ThemeManager").getTheme().then(function () {
            var server = mockServer.instance;

            QUnit.start();

            commonsTests.executeAll(server);
            userTests.executeAll(server);
            mockTests.executeAll(server);
        });
    });
});