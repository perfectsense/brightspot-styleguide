const fs = require('fs-extra')
const path = require('path')
const extensionPattern = '{hbs,json}'
const file = require('gulp-file')
const filter = require('gulp-filter')
const less = require('gulp-less')
const glob = require('glob')

module.exports = {
  registerModule: (styleguide) => {
    let gulp = styleguide._gulp;

    gulp.task('styleguide:build:server:fonts', [ ], () => {
      return gulp.src(path.join(path.dirname(require.resolve('font-awesome/package.json')), 'fonts', '*'))
              .pipe(gulp.dest(styleguide.path.build('fonts')))
    })

    gulp.task('styleguide:build:server:css', [], () => {
      return gulp.src(path.join(__dirname, '..', '..', '..', 'styleguide', 'styleguide.less'))
              .pipe(less())
              .pipe(gulp.dest(styleguide.path.build()))
    })

    gulp.task('styleguide:build:server', [ 'styleguide:build:server:fonts', 'styleguide:build:server:css' ], () => {
    })

    gulp.task('styleguide:build', [ 'styleguide:postcopy:templates', 'styleguide:build:server' ], () => {
    })
  }
}
