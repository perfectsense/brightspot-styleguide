const fs = require('fs')
const path = require('path')
const xml2js = require('xml2js')
const zip = require('gulp-zip')

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
    const pomFile = path.resolve('pom.xml')

    if (fs.existsSync(pomFile)) {
      xml2js.parseString(fs.readFileSync(pomFile), { async: false }, (error, pomXml) => {
        if (error) {
          throw error
        }

        gulp.src(`${styleguide.path.build()}/**`)
          .pipe(zip(`${pomXml.project.artifactId}-${pomXml.project.version}.zip`))
          .pipe(gulp.dest(path.join(styleguide.path.build(), '..')))
      })
    }
  })
}
