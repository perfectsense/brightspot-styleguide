const path = require('path')

const logger = require('../logger')

module.exports = {
  registerModule: (styleguide) => {
    const gulp = styleguide._gulp
    const task = styleguide.task
    const source = styleguide.config.source

    function logChange(file) {
      logger.success(`Changed: ${file.path}`)
    }

    gulp.task(task.watch.html(), [ task.build.html() ], () => {
      return gulp.watch(path.join(source, '**/*.{hbs,json}'), [ task.build.html() ])
        .on('change', logChange)
    })

    gulp.task(task.watch.js(), [ task.build.project() ], () => {
      return gulp.watch(path.join(source, '**/*.js'), [ task.build.project() ])
        .on('change', logChange)
    })

    gulp.task(task.watch.less(), [ task.build.project() ], () => {
      return gulp.watch(path.join(source, '**/*.less'), [ task.build.project() ])
        .on('change', logChange)
    })

    gulp.task(task.watch.all(), [
      task.build.all(),
      task.watch.html(),
      task.watch.js(),
      task.watch.less()

    ], () => {
    })
  }
}
