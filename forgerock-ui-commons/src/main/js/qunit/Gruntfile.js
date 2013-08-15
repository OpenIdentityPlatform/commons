/*global module*/
module.exports = function(grunt) {

    grunt.initConfig({
        watch: {
            files: ['../**/*.js', '../**/*.html', '../**/*.json', '../**/*.less', '../**/*.css', '!../qunit/node_modules/**/*', 'tests/*.js', 'mocks/*.js'],
            tasks: ['qunit']
        },
        qunit: {            
            all: ['index.html']
        },
        notify_hooks: {
            options: {
                enabled: true,
                title: "QUnit Tests" // defaults to the name in package.json, or uses project's directory name, you can change to the name of your project
            }
        }
    });

    grunt.loadNpmTasks('grunt-contrib-qunit');

    grunt.loadNpmTasks('grunt-contrib-watch');
    
    grunt.loadNpmTasks('grunt-notify');

    grunt.task.run('notify_hooks');
    grunt.registerTask('default', ['watch']);

};
