const path = require('path')
const fs = require('fs')
const logger = require('./logger')
const combiner = require('stream-combiner2')
const notify = require('gulp-notify')
const parseString = require('xml2js').parseString

let defaults = {
  'host': 'localhost',
  'port': '3000',
  'project-path': process.cwd(),
  'project-src-path': 'styleguide',
  'project-build-path': path.join(process.cwd(), 'styleguide', '_build'),
  'project-config-path': path.join(process.cwd(), 'styleguide', '_config.json'),
  'maven-pom': 'pom.xml'
}

let Styleguide = function (gulp, settings = { }) {
  this.gulp = gulp

  // Register Styleguide tasks with gulp
  require('./util').loadModules('./gulp/tasks', this)

  // Apply bsp-styleguide config file overrides
  this.config = fs.readFileSync(path.join(__dirname, '../styleguide', '_config.json'), 'utf8')
  if (this.config) {
    this.config = JSON.parse(this.config)
  }

  this.config = Object.assign(defaults, this.config, settings)

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

  parseString(fs.readFileSync(path.join(this.config['project-path'], this.config['maven-pom'])), { async: false }, (err, pomXml) => {
    if (err) {
      console.log('ERROR: ' + err)
    }

    let targetName = `${pomXml.project.artifactId}-${pomXml.project.version}`
    let targetPath = path.join(this.config['project-path'], 'target', targetName, 'gulp')
    this.config['project-build-path'] = targetPath
  })

  process.title = this.config.title || 'styleguide'

  if (this.config.daemon) {
    require('daemon')()
  }

  if (this.config._ && this.config._.length > 0) {
    this.config['project-path'] = this.config._[0]
  }

  this.task = {
    lint: {
      less: () => 'lint:less',
      js: () => 'lint:js',
      json: () => 'lint:json'
    }
  }

  this.path = {
    src: (glob) => path.join(this.config['project-path'], this.config['project-src-path'], glob || ''),
    build: () => this.config['project-build-path']
  }

  this.notify = (_message, _options = null) => {
    let options = Object.assign({ }, {
      icon: false,
      message: `${_message}: <%= file.relative %>`,
      title: 'Brightspot Styleguide',
      sound: 'Purr',
      onLast: true
    }, _options)

    return combiner(
      notify(options)
    )
  }
}

Styleguide.prototype.serve = function (settings) {
  return require('./server')(Object.assign({ }, this.config, settings))
}

module.exports = Styleguide
