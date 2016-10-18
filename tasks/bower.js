const bower = require('gulp-bower');
const bowerFiles = require('main-bower-files');
const gulp = require('gulp');

module.exports.bowerInstall = function () {
    return bower({ directory: './_build/bower_components', cwd: '.' });
}

module.exports.copyBower = function() {
    return gulp.src(bowerFiles({ paths: {
                        bowerDirectory: './_build/bower_components',
                        bowerJson: './bower.json'
                    }}))
                .pipe(gulp.dest('./_build'));
}
