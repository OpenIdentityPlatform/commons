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
 * Portions copyright 2020-2025 3A Systems LLC.
 */

module.exports = function (grunt) {
    grunt.loadNpmTasks("grunt-contrib-less");
    grunt.loadNpmTasks('grunt-contrib-qunit');
    grunt.loadNpmTasks("grunt-contrib-requirejs");
    grunt.loadNpmTasks("grunt-contrib-watch");
    grunt.loadNpmTasks("grunt-eslint");
    grunt.loadNpmTasks('grunt-notify');
    grunt.loadNpmTasks("grunt-sync");

    var targetDirectory = "target/www",
        testTargetDirectory = "target/test",
        sourceDirectory = "src/main/js",
        watchDirs = [
            "../commons/src/main/js",
            "../commons/src/main/resources",
            "../user/src/main/js",
            "../user/src/main/resources",
            "src/main/js",
            "src/main/resources"
        ],
        testWatchDirs = {
            "src/test/qunit": "/",
            "../commons/src/test/qunit": "/tests/commons/",
            "../user/src/test/qunit": "/tests/user/"
        };

    grunt.initConfig({
        eslint: {
            /**
             * Check the JavaScript source code for common mistakes and style issues.
             */
            lint: {
                src: [
                    "src/main/js/**/*.js"
                    //"src/test/js/**/*.js"
                ],
                options: {
                    format: require.resolve("eslint-formatter-warning-summary")
                }
            }
        },
        less: {
            /**
             * Compile LESS source code into minified CSS files.
             */
            compile: {
                files: [{
                    src: targetDirectory + "/css/structure.less",
                    dest: targetDirectory + "/css/structure.css"
                }, {
                    src: targetDirectory + "/css/theme.less",
                    dest: targetDirectory + "/css/theme.css"
                }],
                options: {
                    compress: true,
                    plugins: [
                        new (require("less-plugin-clean-css"))({})
                    ],
                    relativeUrls: true
                }
            }
        },
        qunit: {
            all: [testTargetDirectory + "/index.html"],
            options: {
                timeout: 20000,
                puppeteer: {
                    ignoreDefaultArgs: true,
                    args: [
                        "--headless=new",
                        "--allow-file-access-from-files",
                        "--disable-dev-shm-usage",
                        "--no-sandbox",
                        "--disable-setuid-sandbox"
                    ]
                }
            }
        },
        requirejs: {
            /**
             * Concatenate and uglify the JavaScript.
             */
            compile: {
                options: {
                    baseUrl: targetDirectory,
                    mainConfigFile: sourceDirectory + "/main.js",
                    out: targetDirectory + "/main.js",
                    include: ["main"],
                    preserveLicenseComments: false,
                    generateSourceMaps: true,
                    optimize: "uglify2",
                    excludeShallow: [
                        // These files are excluded from optimization so that the UI can be customized without having to
                        // repackage it.
                        "config/AppConfiguration",

                        // Exclude mock project dependencies to create a more representative bundle.
                        "mock/Data",
                        "sinon"
                    ]
                }
            }
        },
        notify_hooks: {
            options: {
                enabled: true,
                title: "UI QUnit Tests"
            }
        },
        /**
         * Sync is used during development.
         */
        sync: {
            /**
             * Copy all the sources and resources from this project and all dependencies into the target directory.
             */
            source: {
                files: watchDirs.map(function (dir) {
                    return {
                        cwd: dir,
                        src: ["**"],
                        dest: targetDirectory
                    };
                }),
                verbose: true,
                compareUsing: "md5"
            },
            test: {
                files: Object.keys(testWatchDirs).map(function (dir) {
                    return {
                        cwd: dir,
                        src: ["**"],
                        dest: testTargetDirectory + testWatchDirs[dir]
                    };
                }),
                verbose: true,
                compareUsing: "md5"
            }
        },
        watch: {
            /**
             * Redeploy whenever any source files change.
             */
            source: {
                files: watchDirs.map(function (dir) {
                    return dir + "/**";
                }),
                tasks: ["build-dev"]
            },
            test: {
                files: Object.keys(testWatchDirs).map(function (dir) {
                    return dir + "/**";
                }),
                tasks: ["build-dev"]
            }
        }
    });

    grunt.registerTask("build", ["eslint", "less", "requirejs", "sync:test", "qunit"]);
    grunt.registerTask("build-dev", ["less", "sync", "qunit"]);
    grunt.registerTask("dev", ["build-dev", "watch"]);
    grunt.registerTask("default", "dev");
};
