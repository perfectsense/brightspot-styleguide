const paths = require('./paths.js');
const gulp = require('gulp');

module.exports.copySrc = function () {
    return gulp.src(['!' + paths.buildRoot + '/**', '!node_modules/**', '!gulpfile.js', './**/*.{js,less,css,hbs}'])
        .pipe(gulp.dest(paths.buildRoot));
}
