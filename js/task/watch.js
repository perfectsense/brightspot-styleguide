const logger = require('../logger')

module.exports = (styleguide, gulp) => {
  styleguide.watch = () => {
    styleguide.watch.html()
    styleguide.watch.js()
    styleguide.watch.less()
  }

  function onChange (file) {
    logger.info(`Changed: ${file.path}`)
  }

  styleguide.watch.html = () => {
    gulp.watch('**/*.{hbs,json}', { cwd: styleguide.path.source() }, [ styleguide.task.ui() ])
      .on('change', onChange)
  }

  // JS and Less tasks might not be defined.
  const deps = name => styleguide.buildDependencies.includes(name) ? [ name ] : [ ]

  styleguide.watch.js = () => {
    gulp.watch('**/*.js', { cwd: styleguide.path.source() }, deps(styleguide.task.js()))
      .on('change', onChange)
  }

  styleguide.watch.less = () => {
    gulp.watch('**/*.less', { cwd: styleguide.path.source() }, deps(styleguide.task.less()))
      .on('change', onChange)
  }
}
