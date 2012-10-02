/*global define, require*/

require.config({
    "packages" : [ {
        name : "qunit",
        main : "qunit-amd-1.5.0-alpha-1",
        location : "js/org/codehaus/mojo/qunit-amd/1.5.0-alpha-1/qunit-amd-1.5.0-alpha-1"
    }, {
        name : "xdate",
        main : "xdate-0.7-min",
        location : "libs"
    }]
});

require([
    "org/forgerock/commons/ui/common/util/DateUtil",
    "qunit"
], function(dateUtil, qu) {
     
    qu.test("passing test", function() {
        QUnit.equal("a", "a");
    });

//    qu.test("results length test", function() {
//        var date = new Date("12-03-2012");
//        QUnit.equal(dateUtil.formatDate(date), "December 03, 2012");
//    });
            
});