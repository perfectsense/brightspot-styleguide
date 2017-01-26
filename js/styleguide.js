const _ = require('lodash')
const commandLineArguments = require('minimist')(process.argv.slice(2))
const fs = require('fs')
const path = require('path')
const plumber = require('gulp-plumber')
const xml2js = require('xml2js')

const logger = require('./logger.js')

let defaults = {
  host: 'localhost',
  port: '3000',
  root: '.',
  jsonSuffix: '.json',
  devices: [
    {
      name: 'Mobile - Portrait',
      icon: 'fa fa-mobile',
      width: 320,
      height: 480
    },
    {
      name: 'Mobile - Landscape',
      icon: 'fa fa-mobile fa-rotate-270',
      width: 480,
      height: 320
    },
    {
      name: 'Tablet - Portrait',
      icon: 'fa fa-tablet',
      width: 768,
      height: 1024
    },
    {
      name: 'Tablet - Landscape',
      icon: 'fa fa-tablet fa-rotate-270',
      width: 1024,
      height: 768
    },
    {
      name: 'Desktop',
      icon: 'fa fa-desktop',
      width: 1200,
      height: 600
    }
  ]
}

module.exports = function Styleguide (gulp, configOverrides = { }) {
  // Merge settings to allow gulpfile.js to override root.
  const config = _.merge({ }, defaults, configOverrides)

  // Make sure that the root is absolute.
  config.root = path.resolve(process.cwd(), config.root)

  // Merge project config if available.
  const configFile = path.join(config.root, 'styleguide/_config.json')

  if (fs.existsSync(configFile)) {
    _.merge(config, JSON.parse(fs.readFileSync(configFile, 'utf8')))
  }

  // Finally merge command line arguments.
  _.merge(config, commandLineArguments)

  if (!config.build) {
    config.build = path.join(config.root, '_build')

    // If within a Maven project, change the default build directory.
    const pomFile = path.join(config.root, 'pom.xml')

    if (fs.existsSync(pomFile)) {
      xml2js.parseString(fs.readFileSync(pomFile), { async: false }, (error, pomXml) => {
        if (error) {
          throw error
        }

        const packaging = pomXml.project.packaging.toString()

        if (packaging === 'jar') {
          config.build = path.join(config.root, 'target/classes')
        } else if (packaging === 'war') {
          config.build = path.join(config.root, 'target', `${pomXml.project.artifactId}-${pomXml.project.version}`)
        }
      })
    }
  }

  // Process options in case this needs to run in the background.
  process.title = config.title || 'styleguide'

  if (config.daemon) {
    require('daemon')()
  }

  // Expose common paths.
  this.path = {
    build: () => config.build,
    root: () => config.root
  }

  // Variables in config to be used in example JSON files.
  this.var = (name) => config.vars ? config.vars[name] : null

  // Serve the generated UI via a local web server.
  this.serve = () => {
    return require('./server')(config)
  }

  const styleguide = this
  const gulpsrc = gulp.src

  // Overrides gulp.src to patch plumber into all gulp src'ed streams for
  // universal task error management for streams
  gulp.src = function () {
    return gulpsrc.apply(gulp, arguments)
      .pipe(plumber({
        errorHandler: function (err) {
          logger.error(err.message, styleguide.isWatching())

          // When watching, fail gracefully
          if (styleguide.isWatching()) {
            this.emit('end')
          } else {
            process.exit(1)
          }
        }
      }))
  }

  // Expose common tasks.
  this.task = { }

  require('./task/clean')(this, gulp)
  require('./task/build')(this, gulp)
  require('./task/lint')(this, gulp)
  require('./task/ui')(this, gulp)
  require('./task/watch')(this, gulp)

  gulp.task('default', [
    this.task.clean(),
    this.task.build(),
    this.task.lint(),
    this.task.ui()
  ])

  gulp.task('styleguide', [ 'default' ], () => {
    this.serve()
    this.watch()
  })
}
