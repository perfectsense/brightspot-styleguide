const paths = require('./paths.js');
const gulp = require('gulp');

module.exports.copySrc = function () {
    return gulp
            .src(['./**/*.{js,less,css}', '!_build/**/*', '!_dist/**/*', '!node_modules/**/*', '!bower_components/**/*', '!gulpfile.js'])
            .pipe(gulp.dest(paths.buildRoot));
}
