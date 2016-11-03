const lesshint = require('gulp-lesshint')
const standard = require('gulp-standard')
const jsonlint = require('gulp-jsonlint')

module.exports = {
  registerModule: (styleguide) => {
    styleguide.gulp.task('lint:less', () => {
      // TODO: soften the file name?
      return styleguide.gulp.src(styleguide.path.src('All.less'))
        .pipe(lesshint({}))
        .pipe(lesshint.reporter(''))
    })

    styleguide.gulp.task('lint:js', () => {
      // TODO: soften the file name?
      return styleguide.gulp.src(styleguide.path.src('All.js'))
        .pipe(standard())
        .pipe(standard.reporter('default', {
          breakOnError: true,
          quiet: false
        }))
    })

    styleguide.gulp.task('lint:json', () => {
      return styleguide.gulp.src(styleguide.path.src('/**/*.json'))
        .pipe(jsonlint())
        .pipe(jsonlint.reporter())
    })
  }
}
