const jsonlint = require('gulp-jsonlint')
const lesshint = require('gulp-lesshint')
const path = require('path')
const standard = require('gulp-standard')

module.exports = (styleguide, gulp) => {
  styleguide.task.lint = () => 'styleguide:lint'

  styleguide.lint = {
    js: () => {
      return gulp.src(path.join(styleguide.path.source(), 'All.js'))
        .pipe(standard())
        .pipe(standard.reporter('default', {
          breakOnError: true,
          quiet: true
        }))
    },

    json: () => {
      return gulp.src(path.join(styleguide.path.source(), '**/*.json'))
        .pipe(jsonlint())
        .pipe(jsonlint.reporter())
    },

    less: () => {
      return gulp.src(path.join(styleguide.path.source(), 'All.less'))
        .pipe(lesshint())
        .pipe(lesshint.reporter(''))
    }
  }

  gulp.task(styleguide.task.lint(), (done) => {
    styleguide.lint.js()
    styleguide.lint.json()
    styleguide.lint.less()
    done()
  })
}
