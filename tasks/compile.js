const gulp = require('gulp');
const less = require('gulp-less');
const uglify = require('gulp-uglify');
const rename = require('gulp-rename');
const sourceMaps = require('gulp-sourcemaps');
const SystemjsBuilder = require('gulp-systemjs-builder');

module.exports.styles = function (path) {
    path = path || [ '_build' ]

    return less({
        paths: path
    })
}

module.exports.scripts = function (config) {
    var builderConfig = config || {
        minify: false,
        mangle: false
    }

    /*,
    externals: ['jquery'],
    globalDeps: {
        'jquery.js': '$'
    }
    */

    var systemConfig = {
        defaultJSExtensions: true
    }

    // SystemJS Config API: https://github.com/systemjs/systemjs/blob/master/docs/config-api.md
    var builder = new SystemjsBuilder('./_build', systemConfig)

    // SystemJS Builder API: https://github.com/systemjs/builder
    return builder
            .buildStatic('All.js', builderConfig)
            .pipe(sourceMaps.init())
            .pipe(gulp.dest('_dist'))
            .pipe(uglify())
            .pipe(rename({extname: '.min.js'}))
            .pipe(sourceMaps.write('.'))
            .pipe(gulp.dest('_dist'))
}
