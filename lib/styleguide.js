const path = require('path')
const fs = require('fs')
const logger = require('./logger')
const combiner = require('stream-combiner2')
const notify = require('gulp-notify')
const parseString = require('xml2js').parseString
const argv = require('minimist')(process.argv.slice(2))

let defaults = {
  'host': 'localhost',
  'port': '3000',
  'project-path': process.cwd(),
  'project-styleguide-dirname': 'styleguide',
  'project-src-path': path.join(process.cwd(), 'styleguide'),
  'project-target-path': path.join(process.cwd(), 'styleguide', '_build'),
  'project-config-path': path.join(process.cwd(), 'styleguide', '_config.json'),
  'maven-pom': 'pom.xml'
}

let Styleguide = function (gulp, settings = { }) {
  this._gulp = gulp

  // Apply bsp-styleguide config file overrides
  this.config = fs.readFileSync(path.join(__dirname, '../styleguide', '_config.json'), 'utf8')
  if (this.config) {
    this.config = JSON.parse(this.config)
  }

  this.config = Object.assign(defaults, this.config, settings, argv)

  // Apply optional project-level config overrides
  if (fs.existsSync(this.config['project-config-path'])) {
    try {
      this.config = Object.assign(this.config, JSON.parse(fs.readFileSync(this.config['project-config-path'], 'utf8')))
      logger.success('Styleguide is configured with: ' + path.resolve(this.config['project-config-path']))
    } catch (err) {
      logger.warn("There was a problem parsing your project's " + this.config['project-config-path'] + ' file... falling back on the default config.')
      return null
    }
  } else {
    logger.success('Styleguide is configured with default "_config.json" file')
    logger.warn('(You can override this by creating a "' + this.config['project-config-path'] + '" file at the root of your project)')
  }

  let pomFile = path.join(this.config['project-path'], this.config['maven-pom'])
  if (fs.existsSync(pomFile)) {
    parseString(fs.readFileSync(pomFile), { async: false }, (err, pomXml) => {
      if (err) {
        console.log('ERROR: ' + err)
      }

      if (pomXml.project.packaging.toString() === 'war') {
        let targetName = `${pomXml.project.artifactId}-${pomXml.project.version}`
        let targetPath = path.join(this.config['project-path'], 'target', targetName)
        this.config['project-mavenTarget-path'] = targetPath
        this.config['project-target-path'] = targetPath
      }
    })
  }

  process.title = this.config.title || 'styleguide'

  if (this.config.daemon) {
    require('daemon')()
  }

  if (this.config._ && this.config._.length > 0) {
    this.config['project-path'] = this.config._[0]
  }

  this.task = {
    copy: {
      templates: () => 'bsp-styleguide:copy:templates'
    },
    lint: {
      less: () => 'bsp-styleguide:lint:less',
      js: () => 'bsp-styleguide:lint:js',
      json: () => 'bsp-styleguide:lint:json'
    },
    watch: () => 'bsp-styleguide:watch'
  }

  this.path = {
    build: () => this.config['project-target-path'],
    mavenTarget: () => this.config['project-mavenTarget-path']
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
