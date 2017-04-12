const _ = require('lodash')
const commandLineArguments = require('minimist')(process.argv.slice(2))
const findParentDir = require('find-parent-dir')
const fs = require('fs')
const path = require('path')
const plumber = require('gulp-plumber')
const xml2js = require('xml2js')

const handlebars = require('./handlebars.js')
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

  // Where's the root that contains the styleguide files?
  const cwd = process.cwd()
  config.root = path.resolve(cwd, config.root)

  // Merge project config if available.
  const configFile = path.join(config.root, 'styleguide/_config.json')

  if (fs.existsSync(configFile)) {
    _.merge(config, JSON.parse(fs.readFileSync(configFile, 'utf8')))
  }

  // Finally merge command line arguments.
  _.merge(config, commandLineArguments)

  // Where's the parent that contains the package.json file?
  // This may be same as root.
  config.parent = config.parent
    ? path.resolve(cwd, config.parent)
    : findParentDir.sync(config.root, 'package.json')

  // Project name and version.
  if (!config.name || !config.version) {
    const packageFile = path.join(config.root, 'package.json')
    const packageJson = fs.existsSync(packageFile)
      ? JSON.parse(fs.readFileSync(packageFile, 'utf8'))
      : null

    if (!config.name) {
      config.name = packageJson ? packageJson.name : path.basename(config.root)
    }

    if (!config.version) {
      if (packageJson) {
        config.version = packageJson.version
      } else {
        const parentPackageFile = path.join(config.parent, 'package.json')
        config.version = fs.existsSync(parentPackageFile)
          ? JSON.parse(fs.readFileSync(parentPackageFile, 'utf8')).version
          : 'unversioned'
      }
    }
  }

  if (!config.build) {
    config.build = path.join(config.root, '_build')

    // If within a Maven project, change the default build directory.
    const pomFile = config.pom
      ? path.resolve(cwd, config.pom)
      : path.join(config.parent, 'pom.xml')

    if (fs.existsSync(pomFile)) {
      xml2js.parseString(fs.readFileSync(pomFile), { async: false }, (error, pom) => {
        if (error) {
          throw error
        }

        const project = pom.project
        let packaging = pom.project.packaging

        if (packaging) {
          packaging = packaging[0]
          let pomBuild

          if (packaging === 'jar') {
            config.build = path.join(path.dirname(pomFile), 'target/classes')
            pomBuild = true
          } else if (packaging === 'war') {
            let version = project.version

            if (!version) {
              let parent = project.parent

              if (parent) {
                parent = parent[0]

                if (parent) {
                  version = parent.version
                }
              }
            }

            version = version ? version[0] : 'unversioned'
            config.build = path.join(path.dirname(pomFile), 'target', `${project.artifactId}-${version}`)
            pomBuild = true
          }

          if (pomBuild && !config.zip) {
            config.zip = path.join(config.build, '..')
          }
        }
      })
    }
  }

  if (!config.zip) {
    config.zip = config.build
  }

  // Process options in case this needs to run in the background.
  process.title = config.title || 'styleguide'

  if (config.daemon) {
    require('daemon')()
  }

  // Expose common paths.
  this.path = {
    build: () => config.build,
    parent: () => config.parent,
    root: () => config.root,
    zip: () => config.zip
  }

  this.project = {
    name: () => config.name,
    version: () => config.version
  }

  this.handlebars = handlebars(this)

  // random seed API
  this.randomSeed = () => config.randomSeed

  // Variables in config to be used in example JSON files.
  this.var = (name) => config.vars ? config.vars[name] : null

  // Serve the generated UI via a local web server.
  this.serve = () => {
    return require('./server')(config)
  }

  this.sketch = { }

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
  require('./task/sketch')(this, gulp)
  require('./task/build')(this, gulp)
  require('./task/lint')(this, gulp)
  require('./task/ui')(this, gulp)
  require('./task/watch')(this, gulp)

  gulp.task('default', [
    this.task.clean(),
    this.task.sketch(),
    this.task.build(),
    this.task.lint(),
    this.task.ui()
  ])

  gulp.task('styleguide', [ 'default' ], () => {
    this.serve()
    this.watch()
  })
}
