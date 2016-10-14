var bower = require('gulp-bower');

module.exports = function(gulp) {
    // Installs the bower dependencies to the _tmp directory
    gulp.task('install:bower', function() {
         return bower({ directory: './_tmp/bower_components', cwd: '.' })
    });
}
