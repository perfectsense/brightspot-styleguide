const logger = require('../logger')

module.exports = (styleguide, gulp) => {
  this.watching = false

  styleguide.watch = () => {
    this.watching = true

    styleguide.watch.html()
    styleguide.watch.js()
    styleguide.watch.less()
  }

  styleguide.isWatching = () => this.watching

  function onChange (file) {
    logger.info(`Changed: ${file.path}`)
  }

  styleguide.watch.html = () => {
    gulp.watch('styleguide/**/*.{hbs,json,md}', { cwd: styleguide.path.root() }, [ styleguide.task.ui() ])
      .on('change', onChange)
  }

  // JS and Less tasks might not be defined.
  const deps = name => styleguide.buildDependencies.includes(name) ? [ name ] : [ ]

  styleguide.watch.js = () => {
    gulp.watch('styleguide/**/*.js', { cwd: styleguide.path.root() }, deps(styleguide.task.js()))
      .on('change', onChange)
  }

  styleguide.watch.less = () => {
    gulp.watch('styleguide/**/*.less', { cwd: styleguide.path.root() }, deps(styleguide.task.less()))
      .on('change', onChange)
  }
}
