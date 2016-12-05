const path = require('path')
const fs = require('fs')
const logger = require('./logger')
const combiner = require('stream-combiner2')
const notify = require('gulp-notify')
const xml2js = require('xml2js')
const argv = require('minimist')(process.argv.slice(2))

let defaults = {
  host: 'localhost',
  port: '3000',
  root: '.',
  jsonSuffix: '.json',
  devices: [
    {
      name: "Mobile - Portrait",
      icon: "fa fa-mobile",
      width: 320,
      height: 480
    },
    {
      name: "Mobile - Landscape",
      icon: "fa fa-mobile fa-rotate-270",
      width: 480,
      height: 320
    },
    {
      name: "Tablet - Portrait",
      icon: "fa fa-tablet",
      width: 768,
      height: 1024
    },
    {
      name: "Tablet - Landscape",
      icon: "fa fa-tablet fa-rotate-270",
      width: 1024,
      height: 768
    },
    {
      name: "Desktop",
      icon: "fa fa-desktop",
      width: 1200,
      height: 600
    }
  ]
}

let Styleguide = function (gulp, settings = { }) {
  this._gulp = gulp
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
          throw error;
        }

        if (pomXml.project.packaging.toString() === 'war') {
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

  this.task = {
    build: () => 'styleguide:build',
    copy: {
      json: () => 'styleguide:copy:json',
      templates: () => 'styleguide:postcopy:templates'
    },
    lint: {
      less: () => 'styleguide:lint:less',
      js: () => 'styleguide:lint:js',
      json: () => 'styleguide:lint:json'
    },
    watch: () => 'styleguide:watch'
  }

  this.path = {
    build: () => config['build']
  }

  this.watch = () => {
    this._gulp.start(this.task.watch())
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

  // Register Styleguide tasks with gulp
  require('./util').loadModules('./gulp/tasks', this)
}

Styleguide.prototype.serve = function (settings) {
  return require('./server')(Object.assign({ }, this.config, settings))
}

module.exports = Styleguide
