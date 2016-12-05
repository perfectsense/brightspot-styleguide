const logger = require('../../logger')
const path = require('path')

module.exports = {
  registerModule: (styleguide) => {
    const gulp = styleguide._gulp
    const config = styleguide.config

    gulp.task('styleguide:watch', () => {
      function logChange(file) {
        logger.success(`Changed: ${file.path}`)
      }

      gulp.watch(path.join(config.source, '**/*.{hbs,json}'), [ styleguide.task.build() ])
        .on('change', logChange)

      gulp.watch(path.join(config.source, '**/*.js'), [ 'js' ])
        .on('change', logChange)

      gulp.watch(path.join(config.source, '**/*.less'), [ 'css' ])
        .on('change', logChange)
    })
  }
}
