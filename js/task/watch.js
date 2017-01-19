const path = require('path')

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
    gulp.watch(path.join(styleguide.path.root(), 'styleguide/**/*.{hbs,json}'), [ styleguide.task.ui() ])
      .on('change', onChange)
  }

  // JS and Less tasks might not be defined.
  const deps = name => styleguide.buildDependencies.includes(name) ? [ name ] : [ ]

  styleguide.watch.js = () => {
    gulp.watch(path.join(styleguide.path.root(), 'styleguide/**/*.js'), deps(styleguide.task.js()))
      .on('change', onChange)
  }

  styleguide.watch.less = () => {
    gulp.watch(path.join(styleguide.path.root(), 'styleguide/**/*.less'), deps(styleguide.task.less()))
      .on('change', onChange)
  }
}
