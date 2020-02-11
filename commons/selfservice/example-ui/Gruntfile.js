/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014 ForgeRock AS. All Rights Reserved
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

module.exports = function(grunt) {

    grunt.initConfig({
        forgerockui: process.env.FORGEROCK_UI_SRC,
        watch: {
            sync_and_test: {
                files: [
                    '<%= forgerockui %>/forgerock-ui-commons/src/main/js/**',
                    '<%= forgerockui %>/forgerock-ui-commons/src/main/resources/**',
                    //'<%= forgerockui %>/forgerock-ui-commons/src/test/qunit/**',
                    '<%= forgerockui %>/forgerock-ui-user/src/main/js/**',
                    '<%= forgerockui %>/forgerock-ui-user/src/main/resources/**',
                    //'<%= forgerockui %>/forgerock-ui-user/src/test/qunit/**',
                    'src/main/js/**',
                    'src/main/resources/**',
                    //'src/test/qunit/**',
                    //'src/test/resources/**',
                    //'src/test/js/**'
                ],
                tasks: [ 'sync', 'less' ]//, 'qunit' ]
            }
        },
        less: {
            mock: {
                files: {
                    "target/www/css/theme.css": "target/www/css/theme.less",
                    "target/www/css/structure.css": "target/www/css/structure.less"
                }
            }
        },
        sync: {
            commons: {
                files: [
                    {
                        cwd     : '<%= forgerockui %>/forgerock-ui-commons/src/main/js',
                        src     : ['**'],
                        dest    : 'target/www'
                    },
                    {
                        cwd     : '<%= forgerockui %>/forgerock-ui-commons/src/main/resources',
                        src     : ['**'],
                        dest    : 'target/www'
                    }/*,
                    {
                        cwd     : '<%= forgerockui %>/forgerock-ui-commons/src/test/qunit',
                        src     : ['**'],
                        dest    : 'target/test/tests'
                    }*/
                ]
            },
            user: {
                files: [
                    {
                        cwd     : '<%= forgerockui %>/forgerock-ui-user/src/main/js',
                        src     : ['**'],
                        dest    : 'target/www'
                    },
                    {
                        cwd     : '<%= forgerockui %>/forgerock-ui-user/src/main/resources',
                        src     : ['**'],
                        dest    : 'target/www'
                    }/*,
                    {
                        cwd     : '<%= forgerockui %>/forgerock-ui-user/src/test/qunit',
                        src     : ['**'],
                        dest    : 'target/test/tests'
                    }*/
                ]
            },
            selfService: {
                files: [
                    {
                        cwd     : 'src/main/js',
                        src     : ['**'],
                        dest    : 'target/www'
                    },
                    {
                        cwd     : 'src/main/resources',
                        src     : ['**'],
                        dest    : 'target/www'
                    }/*,
                    {
                        cwd     : 'resources',
                        src     : ['css/**', 'qunit.html'],
                        dest    : 'target/test'
                    },
                    {
                        cwd     : 'qunit',
                        src     : ['**'],
                        dest    : 'target/test/tests'
                    },
                    {
                        cwd     : 'js',
                        src     : ['**'],
                        dest    : 'target/test'
                    }*/
                ]
            },
            deploy : {
                files: [
                    {
                        cwd     : 'target/www',
                        src     : ['**'],
                        dest    : '../forgerock-selfservice-example/target/webapp'
                    }
                ]
            }
        }
        /*,
        qunit: {
            all: ['target/test/qunit.html']
        },
        notify_hooks: {
            options: {
                enabled: true,
                title: "ForgeRock UI QUnit Tests"
            }
        }*/
    });

    //grunt.loadNpmTasks('grunt-contrib-qunit');
    //grunt.loadNpmTasks('grunt-notify');
    //grunt.task.run('notify_hooks');
    grunt.loadNpmTasks('grunt-contrib-watch');
    grunt.loadNpmTasks('grunt-sync');
    grunt.loadNpmTasks('grunt-contrib-less');

    grunt.registerTask('default', ['sync', 'less', 'watch']);

};
