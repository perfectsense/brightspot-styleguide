const del = require('del');
const gulp = require('gulp');

module.exports.clean = function (src) {
    if (!src) {
        src = ['_dist/**/*', '_build/**/*'];
    }

    return del(src);
}

module.exports.copySrc = function () {
    return gulp.src(['!_build/**', '!gulpfile.js', './**/*.{js,less,css}'])
        .pipe(gulp.dest('./_build/'));
}
