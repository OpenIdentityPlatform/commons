/*global define, require*/

require.config({
    "packages" : [ {
        name : "qunit",
        main : "qunit-amd-1.2.0-SNAPSHOT",
        location : "js/org/codehaus/mojo/qunit-amd/1.2.0-SNAPSHOT/qunit-amd-1.2.0-SNAPSHOT"
    }, {
        name : "xdate",
        main : "xdate-0.7-min",
        location : "libs"
    }, {
        name : "moment",
        main : "moment-1.7.2-min",
        location : "libs"
    }]
});

require([
    "org/forgerock/commons/ui/common/util/DateUtil",
    "qunit"
], function(dateUtil, qu) {
     
    qu.test("passing test", function() {
        QUnit.equals("a", "a");
    });

    qu.test("results length test", function() {
        var date = new Date("October 13, 1975 11:13:00");
        QUnit.equals(dateUtil.formatDate(date), "October 13, 1975");
    });
            
});