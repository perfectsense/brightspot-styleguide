const lesshint = require('gulp-lesshint')
const standard = require('gulp-standard')
const jsonlint = require('gulp-jsonlint')
const path = require('path')

module.exports = {
  registerModule: (styleguide) => {
    let gulp = styleguide._gulp;

    gulp.task('styleguide:build', [ 'styleguide:postcopy:templates', 'styleguide:copy:styleguide' ], () => {
    })
  }
}
