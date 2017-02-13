const jsonlint = require('gulp-jsonlint')
const lesshint = require('gulp-lesshint')
const mergeStream = require('merge-stream')
const path = require('path')
const standard = require('gulp-standard')

module.exports = (styleguide, gulp) => {
  styleguide.task.lint = () => 'styleguide:lint'

  styleguide.lint = {
    js: () => {
      return gulp.src(path.join(styleguide.path.root(), 'styleguide/**/*.js'))
        .pipe(standard())
        .pipe(standard.reporter('default', {
          breakOnError: true,
          quiet: true
        }))
    },

    json: () => {
      return gulp.src(path.join(styleguide.path.root(), 'styleguide/**/*.json'))
        .pipe(jsonlint())
        .pipe(jsonlint.failAfterError())
    },

    less: () => {
      return gulp.src(path.join(styleguide.path.root(), 'styleguide/**/*.less'))
        .pipe(lesshint({
          configPath: path.join(__dirname, '.lesshintrc')
        }))
        .pipe(lesshint.reporter(''))
    }
  }

  gulp.task(styleguide.task.lint(), () => {
    return mergeStream(
      styleguide.lint.js(),
      styleguide.lint.json(),
      styleguide.lint.less())
  })
}
