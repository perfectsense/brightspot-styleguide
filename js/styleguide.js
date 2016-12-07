const fs = require('fs')
const notify = require('gulp-notify')
const argv = require('minimist')(process.argv.slice(2))
const path = require('path')
const requireDir = require('require-dir')
const combiner = require('stream-combiner2')
const xml2js = require('xml2js')

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

module.exports = function Styleguide (gulp, settings = { }) {
  const config = this.config = Object.assign(defaults, settings, argv)

  if (!config.source) {
    config.source = path.join(config.root, 'styleguide')
  }

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

  const configFile = path.join(config.source, '_config.json')

  if (fs.existsSync(configFile)) {
    Object.assign(config, JSON.parse(fs.readFileSync(configFile, 'utf8')))
  }

  process.title = config.title || 'styleguide'

  if (config.daemon) {
    require('daemon')()
  }

  this.path = {
    build: () => config['build']
  }

  this.watch = () => {
    this._gulp.start(this.task.watch.all())
  }

  this.notify = (_message, _options = null) => {
    let options = Object.assign({ }, {
      icon: false,
      message: `${_message}: <%= file.relative %> \u{1F44D}`,
      title: 'Brightspot Styleguide',
      sound: 'Purr',
      onLast: true
    }, _options)

    return combiner(
      notify(options)
    )
  }

  this.task = {
    build: {
      all: () => 'styleguide:build',
      examples: () => 'styleguide:build:examples',
      html: () => 'styleguide:build:html',
      project: () => 'styleguide:build:project',

      ui: {
        all: () => 'styleguide:build:ui:all',
        less: () => 'styleguide:build:ui:less',
        fonts: () => 'styleguide:build:ui:fonts'
      }
    },

    copy: {
      all: () => 'styleguide:copy',
      html: () => 'styleguide:copy:html',
      sourced: () => 'styleguide:copy:sourced'
    },

    lint: {
      all: () => 'styleguide:lint:all',
      js: () => 'styleguide:lint:js',
      json: () => 'styleguide:lint:json',
      less: () => 'styleguide:lint:less'
    },

    watch: {
      all: () => 'styleguide:watch',
      html: () => 'styleguide:watch:html',
      js: () => 'styleguide:watch:js',
      less: () => 'styleguide:watch:less'
    }
  }

  const gulpModules = requireDir('./gulp', { recurse: false })

  Object.keys(gulpModules).forEach(name => {
    gulpModules[name](this, gulp)
  })

  gulp.task('default', [ this.task.build.all() ])

  this.serve = () => {
    return require('./server')(Object.assign({ }, this.config, settings))
  }

  gulp.task('styleguide', [ 'default', this.task.watch.all() ], () => {
    this.serve()
  })
}
