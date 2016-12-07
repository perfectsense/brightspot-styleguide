const gulp = require('gulp')
const standard = require('gulp-standard')

gulp.task('standard', () => {
  return gulp.src('js/**/*.js')
    .pipe(standard())
    .pipe(standard.reporter('default', {
      breakOnError: true,
      quiet: true
    }))
})

gulp.task('default', [ 'standard' ])
