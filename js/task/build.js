const path = require('path')

module.exports = (styleguide, gulp) => {
  Object.assign(styleguide.task, {
    build: () => 'styleguide:build',
    js: () => 'js',
    less: () => 'less'
  })

  // Intercept gulp.task to allow dynamic dependencies on the build task.
  const originalGulpTask = gulp.task
  styleguide.buildExtras = [ styleguide.task.js(), styleguide.task.less() ]
  styleguide.buildDependencies = [ styleguide.task.clean() ]

  styleguide.task.extra = name => {
    styleguide.buildExtras.push(name)
    return name
  }

  gulp.task = function () {
    const name = arguments[0]

    if (styleguide.buildExtras.includes(name)) {
      styleguide.buildDependencies.push(name)
    }

    originalGulpTask.apply(this, arguments)
  }

  gulp.task(styleguide.task.build(), styleguide.buildDependencies, () => {
    return gulp.src('src/main/resources/**')
      .pipe(gulp.dest(path.join(styleguide.path.build(), '_resource')))
  })
}
