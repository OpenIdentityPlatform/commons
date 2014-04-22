module.exports = function(grunt) {

    grunt.initConfig({
        watch: {
            sync_and_test: {
                files: [
                    '../../../forgerock-ui-commons/src/main/js/**',
                    '../../../forgerock-ui-commons/src/main/resources/**',
                    '../../../forgerock-ui-commons/src/test/qunit/**',
                    '../../../forgerock-ui-user/src/main/js/**',
                    '../../../forgerock-ui-user/src/main/resources/**',
                    '../../../forgerock-ui-user/src/test/qunit/**',
                    '../main/js/**',
                    '../main/resources/**',
                    '../test/qunit/**',
                    'resources/**',
                    'js/**'
                ],
                tasks: [ 'sync', 'qunit' ]
            }
        },
        sync: {
            commons: {
                files: [
                    {
                        cwd     : '../../../forgerock-ui-commons/src/main/js',
                        src     : ['**'], 
                        dest    : '../../target/www'
                    },
                    {
                        cwd     : '../../../forgerock-ui-commons/src/main/resources',
                        src     : ['**'], 
                        dest    : '../../target/www'
                    },
                    {
                        cwd     : '../../../forgerock-ui-commons/src/test/qunit',
                        src     : ['**'], 
                        dest    : '../../target/test/tests'
                    }
                ]
            },
            user: {
                files: [
                    {
                        cwd     : '../../../forgerock-ui-user/src/main/js',
                        src     : ['**'], 
                        dest    : '../../target/www'
                    },
                    {
                        cwd     : '../../../forgerock-ui-user/src/main/resources',
                        src     : ['**'], 
                        dest    : '../../target/www'
                    },
                    {
                        cwd     : '../../../forgerock-ui-user/src/test/qunit',
                        src     : ['**'], 
                        dest    : '../../target/test/tests'
                    }
                ]
            },
            mock: {
                files: [
                    {
                        cwd     : '../main/js',
                        src     : ['**'], 
                        dest    : '../../target/www'
                    },
                    {
                        cwd     : '../main/resources',
                        src     : ['**'], 
                        dest    : '../../target/www'
                    },
                    {
                        cwd     : 'resources',
                        src     : ['css/**', 'qunit.html'], 
                        dest    : '../../target/test'
                    },
                    {
                        cwd     : 'qunit',
                        src     : ['**'], 
                        dest    : '../../target/test/tests'
                    },
                    {
                        cwd     : 'js',
                        src     : ['**'], 
                        dest    : '../../target/test'
                    }
                ]
            }
        },
        qunit: {
            all: ['../../target/test/qunit.html']
        },
        notify_hooks: {
            options: {
                enabled: true,
                title: "ForgeRock UI QUnit Tests"
            }
        }
    });

    grunt.loadNpmTasks('grunt-contrib-qunit');

    grunt.loadNpmTasks('grunt-contrib-watch');
    
    grunt.loadNpmTasks('grunt-notify');

    grunt.loadNpmTasks('grunt-sync');

    grunt.task.run('notify_hooks');
    grunt.registerTask('default', ['watch']);

};
