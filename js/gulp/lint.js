const jsonlint = require('gulp-jsonlint')
const lesshint = require('gulp-lesshint')
const path = require('path')
const standard = require('gulp-standard')

module.exports = (styleguide, gulp) => {
  const task = styleguide.task
  const source = styleguide.config.source

  gulp.task(task.lint.js(), () => {
    return gulp.src(path.join(source, 'All.js'))
      .pipe(standard())
      .pipe(standard.reporter('default', {
        breakOnError: true,
        quiet: true
      }))
  })

  gulp.task(task.lint.json(), () => {
    return gulp.src(path.join(source, '**/*.json'))
      .pipe(jsonlint())
      .pipe(jsonlint.reporter())
  })

  gulp.task(task.lint.less(), () => {
    return gulp.src(path.join(source, 'All.less'))
      .pipe(lesshint())
      .pipe(lesshint.reporter(''))
  })

  gulp.task(task.lint.all(), [
    task.lint.js(),
    task.lint.json(),
    task.lint.less()

  ], () => {
  })
}
