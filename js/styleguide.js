const _ = require('lodash')
const commandLineArguments = require('minimist')(process.argv.slice(2))
const findParentDir = require('find-parent-dir')
const fs = require('fs')
const glob = require('glob')
const path = require('path')
const plumber = require('gulp-plumber')
const traverse = require('traverse')
const xml2js = require('xml2js')

const handlebars = require('./handlebars.js')
const logger = require('./logger.js')
const resolver = require('./resolver.js')

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
  const styleguide = this

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
    config: () => configFile,
    parent: () => config.parent,
    root: () => config.root,
    zip: () => config.zip
  }

  this.project = {
    name: () => config.name,
    version: () => config.version
  }

  // Display name overrides.
  const displayNames = { }

  if (config.displayNames) {
    Object.keys(config.displayNames).forEach(p => {
      displayNames[resolver.path(styleguide.path.build(), configFile, p)] = config.displayNames[p]
    })
  }

  this.displayNames = () => _.cloneDeep(displayNames)

  // Image sizes
  this.getImageSize = config.imageSizes ? (name) => config.imageSizes[name] : (name) => null

  const imageSizeContexts = { }

  if (config.imageSizeContexts) {
    traverse(config.imageSizeContexts).forEach(function (value) {
      if (typeof value === 'string') {
        const imageSize = styleguide.getImageSize(value)

        if (!imageSize) {
          throw new Error(`Can't find an image size named [${value}]!`)
        }

        const field = this.key
        let map = imageSizeContexts[field] = imageSizeContexts[field] || { }
        let count = 0

        for (let parent = this.parent; parent.parent; parent = parent.parent) {
          map = map[parent.key] = map[parent.key] || { }
          count++
        }

        map.name = value
        map.count = count
        map.area = (imageSize.width || imageSize.height) * (imageSize.height || imageSize.width)
      }
    })
  }

  const originalTemplates = { }

  if (config.styles) {
    Object.keys(config.styles).forEach(originalTemplate => {
      const relativeOriginalTemplate = '/' + path.relative(config.root, resolver.path(config.root, configFile, originalTemplate))
      const templates = config.styles[originalTemplate].templates

      if (templates && templates.length > 0) {
        templates.forEach(template => {
          const relativeTemplate = '/' + path.relative(config.root, resolver.path(config.root, configFile, template.template))
          originalTemplates[relativeTemplate] = relativeOriginalTemplate
        })
      }
    })
  }

  this.getImageSizeName = node => {
    const chain = [ ]
    const field = node.key
    const context = imageSizeContexts[field]

    chain.unshift(field)

    if (context) {
      let candidates = [ ]

      const find = template => {
        chain.unshift(template)

        candidates.forEach((candidate, i) => {
          const next = candidate[template]

          if (next) {
            candidates[i] = next
          }
        })

        const first = context[template]

        if (first) {
          candidates.push(first)
        }
      }

      for (let parent = node.parent; parent; parent = parent.parent) {
        let template = parent.node._template

        if (template) {
          template = '/' + path.relative(config.build, template).replace('node_modules/' + config.name + '/', '')

          find(template)

          const originalTemplate = originalTemplates[template]

          if (originalTemplate) {
            find(originalTemplate)
          }
        }

        const index = parseInt(parent.key, 10)

        if (!isNaN(index)) {
          find(parent.parent.key + ':' + index)
        }
      }

      candidates = candidates.filter(c => c.count)

      if (candidates.length > 0) {
        const maxCount = Math.max(...candidates.map(c => c.count))
        candidates = candidates.filter(c => c.count === maxCount)

        candidates.sort((x, y) => x.area - y.area)
        return candidates[0].name
      }
    }

    throw new Error(`Can't find an appropriate image size name for [${chain.join(', ')}]!`)
  }

  // Styles.
  this.styles = () => config.styles ? _.cloneDeep(config.styles) : null

  // Handlebars renderer.
  this.handlebars = handlebars(this)

  // Random seed API.
  this.randomSeed = () => config.randomSeed

  // Variables in config to be used in example JSON files.
  this.var = (name) => config.vars ? config.vars[name] : null

  // Serve the generated UI via a local web server.
  this.serve = () => {
    return require('./server')(config)
  }

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

  // Temporary task to help in migrating old image syntax into the new config.
  gulp.task('create-image-size-config', done => {
    const configPath = path.join(this.path.root(), 'styleguide/_config.json')
    const config = JSON.parse(fs.readFileSync(configPath, 'utf8'))

    if (config.imageSizes || config.imageSizeContexts) {
      done()
      return
    }

    const styles = config.styles
    const originalTemplates = { }

    if (styles) {
      Object.keys(styles).forEach(originalTemplate => {
        const templates = styles[originalTemplate].templates

        if (templates) {
          templates.forEach(template => {
            originalTemplates[template.template] = originalTemplate
          })
        }
      })
    }

    const imageSizes = { }
    const imageSizeContexts = { }
    const resolve = (parent, template) => {
      return template ? '/' + path.relative(this.path.root(), resolver.path(this.path.root(), parent, template)) : null
    }

    glob.sync(path.join(this.path.root(), 'styleguide/**/*.json')).forEach(examplePath => {
      const exampleJson = JSON.parse(fs.readFileSync(examplePath, 'utf8'))
      const exampleTemplate = resolve(examplePath, exampleJson._template)
      let changed

      traverse(exampleJson).forEach(function (value) {
        if (typeof value === 'string') {
          this.update(value.replace(/\{\{\s*image\s*\(\s*(\d+|\[[^]]+])\s*,\s*(\d+|\[[^]]+])\s*\)\s*}}/, (match, width, height) => {
            changed = true
            width = parseInt(width, 10)
            height = parseInt(height, 10)
            const name = width + 'x' + height
            const imageSize = imageSizes[name] = { }

            if (width) {
              imageSize.width = width
            } else {
              imageSize.previewWidth = 100
            }

            if (height) {
              imageSize.height = height
            } else {
              imageSize.previewHeight = 100
            }

            const x = imageSizeContexts[exampleTemplate] = imageSizeContexts[exampleTemplate] || { }
            let y

            if (this.parent.isRoot) {
              y = x
            } else {
              let template = resolve(examplePath, this.parent.node._template)
              template = originalTemplates[template] || template
              y = x[template] = x[template] || { }
            }

            if (y[this.key]) {
              if (y[this.key] !== name) {
                for (let parent = this.parent; parent; parent = parent.parent) {
                  const index = parseInt(parent.key, 10)

                  if (!isNaN(index)) {
                    const grandparent = parent.parent
                    const grandparentTemplate = grandparent.parent.node._template

                    if (grandparentTemplate) {
                      const field = grandparent.key + ':' + index
                      const z = y[field] = y[field] || { }
                      z[this.key] = name

                      break
                    }
                  }
                }
              }
            } else {
              y[this.key] = name
            }

            return '{{image}}'
          }))
        }
      })

      if (changed) {
        fs.writeFileSync(examplePath, JSON.stringify(exampleJson, null, '  '))
      }
    })

    config.imageSizes = imageSizes
    config.imageSizeContexts = imageSizeContexts

    fs.writeFileSync(configPath, JSON.stringify(config, null, '  '))
  })
}
