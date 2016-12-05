const lesshint = require('gulp-lesshint')
const standard = require('gulp-standard')
const jsonlint = require('gulp-jsonlint')
const path = require('path')

module.exports = {
  registerModule: (styleguide) => {
    styleguide._gulp.task('styleguide:lint:less', () => {
      // TODO: soften the file name?
      return styleguide._gulp.src(path.join(styleguide.config['project-src-path'], 'All.less'))
        .pipe(lesshint({}))
        .pipe(lesshint.reporter(''))
    })

    styleguide._gulp.task('styleguide:lint:js', () => {
      // TODO: soften the file name?
      return styleguide._gulp.src(path.join(styleguide.config['project-src-path'], 'All.js'))
        .pipe(standard())
        .pipe(standard.reporter('default', {
          breakOnError: true,
          quiet: true
        }))
    })

    styleguide._gulp.task('styleguide:lint:json', () => {
      return styleguide._gulp.src(path.join(styleguide.config['project-src-path'], '**/*.json'))
        .pipe(jsonlint())
        .pipe(jsonlint.reporter())
    })
  }
}
