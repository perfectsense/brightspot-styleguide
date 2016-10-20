const paths = require('./paths.js');
const bower = require('gulp-bower');
const bowerFiles = require('main-bower-files');
const gulp = require('gulp');

module.exports.bowerInstall = function () {
    return bower({ directory: './bower_components', cwd: '.' });
}

module.exports.copyBower = function() {
    return gulp
            .src(bowerFiles({ paths: {
                bowerDirectory: './bower_components',
                bowerJson: './bower.json'
            }}))
            .pipe(gulp.dest(paths.buildRoot));
}
