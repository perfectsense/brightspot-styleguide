const logger = require('../logger')
/* eslint-disable no-unused-vars */
const dotenv = require('dotenv').config()
/* eslint-enable no-unused-vars */
const path = require('path')
const plugins = require('gulp-load-plugins')()

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
    gulp.watch('styleguide/**/*.{hbs,json}', { cwd: styleguide.path.root() }, [ styleguide.task.ui() ])
      .on('change', onChange)
  }

  // JS and Less tasks might not be defined.
  const deps = name => styleguide.buildDependencies.includes(name) ? [ name ] : [ ]

  styleguide.watch.js = () => {
    gulp.watch('styleguide/**/*.js', { cwd: styleguide.path.root() }, deps(styleguide.task.js()))
      .on('change', onChange)
    // waiting for change in the minified file before copying unminified to minified
    if (process.env.JS_MIN !== 'true') {
      gulp.watch('styleguide/All.min.js', { cwd: styleguide.path.build() })
        .on('change', function (event) {
          logger.info(`COPY .js >> .min.js: ${event.path}`)
          return gulp.src(path.join(styleguide.path.build(), 'styleguide/All.js'), { base: '.' })
            .pipe(plugins.rename({ extname: '.min.js' }))
            .pipe(gulp.dest('.'))
        })
    }
  }

  styleguide.watch.less = () => {
    gulp.watch('styleguide/**/*.less', { cwd: styleguide.path.root() }, deps(styleguide.task.less()))
      .on('change', onChange)
  }
}
