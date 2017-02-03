const del = require('del')
const filter = require('gulp-filter')
const fs = require('fs-extra')
const glob = require('glob')
const gutil = require('gulp-util')
const handlebars = require('handlebars')
const less = require('gulp-less')
const path = require('path')
const through = require('through2')
const traverse = require('traverse')
const plugins = require('gulp-load-plugins')()
const Builder = require('systemjs-builder')

const example = require('../example')
const label = require('../label')
const logger = require('../logger')
const resolver = require('../resolver')

module.exports = (styleguide, gulp) => {
  styleguide.task.ui = () => 'styleguide:ui'

  function getProjectName () {
    return JSON.parse(fs.readFileSync(path.join(styleguide.path.root(), 'package.json'), 'utf8')).name
  }

  function getProjectRootPath () {
    return path.join(styleguide.path.build(), 'node_modules', getProjectName())
  }

  styleguide.ui = {

    // Copy all files related to producing example HTML.
    copy: done => {
      // Pretend that the project is a package.
      const projectFiles = [
        path.join(styleguide.path.root(), 'package.json'),
        path.join(styleguide.path.root(), 'styleguide/**/*.{hbs,json}')
      ]

      const projectRootPath = getProjectRootPath()

      gulp.src(projectFiles, { base: '.' })
        .pipe(gulp.dest(projectRootPath))
        .on('end', () => {
          // Automatically create all files related to styled templates.
          const configPath = path.join(projectRootPath, 'styleguide/_config.json')
          const styledTemplates = { }

          if (fs.existsSync(configPath)) {
            const styles = JSON.parse(fs.readFileSync(configPath, 'utf8')).styles

            if (styles) {
              const rootPath = styleguide.path.root()

              Object.keys(styles).forEach(styledTemplate => {
                const style = styles[styledTemplate]
                const templates = style.templates

                // Create styled example JSON files.
                if (templates) {
                  templates.forEach(template => {
                    const example = template.example || style.example
                    const examplePath = resolver.path(rootPath, configPath, example)
                    const exampleJson = JSON.parse(fs.readFileSync(examplePath, 'utf8'))

                    traverse(exampleJson).forEach(function (value) {
                      if (!value) {
                        return
                      }

                      if (this.key === '_template' &&
                        resolver.path(rootPath, examplePath, value) === resolver.path(rootPath, examplePath, styledTemplate)) {
                        this.update(template.template)
                      } else if ((this.key === '_template' ||
                        this.key === '_wrapper' ||
                        this.key === '_include' ||
                        this.key === '_dataUrl') &&
                        !value.startsWith('/')) {
                        this.update(path.resolve(path.dirname(example), value))
                      }
                    })

                    const styledExamplePath = gutil.replaceExtension(resolver.path(rootPath, configPath, template.template), '.json')

                    fs.mkdirsSync(path.dirname(styledExamplePath))
                    fs.writeFileSync(styledExamplePath, JSON.stringify(exampleJson))
                  })
                }

                // Create the template that delegates to the styled ones.
                styledTemplates[styledTemplate] = ''

                for (let i = templates.length - 1; i > 0; --i) {
                  const template = templates[i]
                  const internalName = template.internalName || template.template
                  const templatePath = resolver.path(rootPath, configPath, template.template)

                  styledTemplates[styledTemplate] += `{{#styledTemplate '${internalName}'}}${fs.readFileSync(templatePath, 'utf8')}{{else}}`
                }

                styledTemplates[styledTemplate] += fs.readFileSync(path.join(styleguide.path.root(), templates[0].template), 'utf8')

                for (let i = templates.length - 1; i > 0; --i) {
                  styledTemplates[styledTemplate] += '{{/styledTemplate}}'
                }
              })
            }
          }

          // Don't copy package.json from modules without the styleguide.
          const onlyStyleguidePackages = filter(file => {
            const filePath = file.path

            return path.basename(filePath) !== 'package.json' ||
              fs.existsSync(path.join(path.dirname(filePath), 'styleguide'))
          })

          const packageFiles = [
            path.join(styleguide.path.root(), 'node_modules/*/package.json'),
            path.join(styleguide.path.root(), 'node_modules/*/styleguide/**/*.{hbs,json}')
          ]

          gulp.src(packageFiles, { base: '.' })
            .pipe(onlyStyleguidePackages)
            .pipe(gulp.dest(styleguide.path.build()))

            // Copy all files related to theming.
            .on('end', () => {
              glob.sync(path.join(projectRootPath, 'styleguide/**/_theme.json'), { absolute: true }).forEach(themeFile => {
                const theme = JSON.parse(fs.readFileSync(themeFile, 'utf8'))
                const rawSource = theme.source

                // Make sure source exists and is resolved.
                if (!rawSource) return

                const source = path.join(styleguide.path.build(), rawSource)
                const themeDir = path.dirname(themeFile)
                const report = {
                  overrides: [ ],
                  errors: [ ]
                }

                // Find all templates within the theme directory.
                glob.sync('**/*.hbs', { cwd: themeDir }).forEach(overridePath => {
                  const sourcePath = path.join(source, overridePath)

                  if (fs.existsSync(sourcePath)) {
                    report.overrides.push({
                      overridePath: path.join(themeDir, overridePath),
                      sourcePath: sourcePath,
                      sourceCopy: path.resolve(path.dirname(sourcePath), '_Source' + path.basename(sourcePath))
                    })
                  } else {
                    report.errors.push({
                      message: `Can't theme [${overridePath}] because it doesn't exist in source at [${sourcePath}]!`
                    })
                  }
                })

                // Move the theme templates to the source.
                if (report.overrides.length > 0) {
                  report.overrides.forEach(override => {
                    fs.move(override.sourcePath, override.sourceCopy, { clobber: true }, error => {
                      if (error) {
                        throw error
                      }

                      fs.move(override.overridePath, override.sourcePath, { clobber: true }, error => {
                        if (error) {
                          throw error
                        }
                      })
                    })
                  })
                }

                // Throw any errors that were detected.
                if (report.errors.length > 0) {
                  throw new Error(report.errors.map(error => `\n${error.message}\n`))
                }

                // Copy all example JSON files into the theme directory.
                glob.sync('**/*.json', { cwd: source }).forEach(examplePath => {
                  if (path.basename(examplePath) !== 'package.json') {
                    const exampleJson = JSON.parse(fs.readFileSync(path.join(source, examplePath), 'utf8'))
                    exampleJson._hidden = theme.hidden

                    traverse(exampleJson).forEach(function (value) {
                      if (!value) {
                        return
                      }

                      if (this.key === '_template' ||
                        this.key === '_wrapper' ||
                        this.key === '_include' ||
                        this.key === '_dataUrl') {
                        if (!value.startsWith('/')) {
                          value = path.resolve(path.dirname(path.resolve(rawSource, examplePath)), value)
                        }

                        if (value.startsWith('/styleguide/')) {
                          value = path.join(rawSource, path.relative('/styleguide/', value))
                        }

                        this.update(value)
                      }

                      const themeExamplePath = path.join(themeDir, examplePath)

                      fs.mkdirsSync(path.dirname(themeExamplePath))
                      fs.writeFileSync(themeExamplePath, JSON.stringify(exampleJson))
                    })
                  }
                })
              })

              // Override styled templates.
              Object.keys(styledTemplates).forEach(styledTemplate => {
                const styledTemplatePath = path.join(styleguide.path.build(), styledTemplate)

                fs.mkdirsSync(path.dirname(styledTemplatePath))
                fs.writeFileSync(styledTemplatePath,
                  '{{#styled}}' +
                  styledTemplates[styledTemplate] +
                  '{{else}}' +
                  fs.readFileSync(styledTemplatePath, 'utf8') +
                  '{{/styled}}')
              })

              done()
            })
        })
    },

    // Copy fonts used by the styleguide UI itself.
    fonts: () => {
      return gulp.src(path.join(path.dirname(require.resolve('font-awesome/package.json')), 'fonts', '*'))
        .pipe(gulp.dest(path.join(styleguide.path.build(), '_styleguide')))
    },

    // Convert example JSON files to HTML.
    html: done => {
      function jsonToHtml (file, encoding, callback) {
        const filePath = file.path
        const fileName = path.basename(filePath)

        if (fileName !== 'package.json' && fileName.slice(0, 1) !== '_') {
          try {
            const html = example(styleguide, filePath)

            if (html) {
              file.base = styleguide.path.build()
              file.contents = Buffer.from(html)
              file.path = gutil.replaceExtension(filePath, '.html')
              this.push(file)
            }
          } catch (err) {
            logger.error(`${err.message} at [${filePath}]!`)
            if (!styleguide.isWatching()) {
              process.exit(1)
            }
          }
        }

        callback()
      }

      // Build the index HTML that serves as the entry to the styleguide UI
      // after all the example HTML files are produced.
      const projectRootPath = getProjectRootPath()

      gulp.src(path.join(projectRootPath, 'styleguide/**/*.json'))
        .pipe(through.obj(jsonToHtml))
        .pipe(gulp.dest(styleguide.path.build()))
        .on('end', () => {
          // Group example HTML files by their path.
          const groupByName = { }

          glob.sync('**/*.html', { cwd: styleguide.path.build() }).forEach(match => {
            const groupName = path.dirname(path.relative(path.join(projectRootPath, 'styleguide'), path.join(styleguide.path.build(), match))).split('/').map(label).join(': ')
            let group = groupByName[groupName]

            if (!group) {
              group = groupByName[groupName] = {
                name: groupName,
                examples: [ ]
              }
            }

            group.examples.push({
              name: label(path.basename(match, '.html')),
              url: '/' + gutil.replaceExtension(match, '.html')
            })
          })

          // Sort the grouping so that the display is alphabetical.
          const groups = [ ]

          Object.keys(groupByName).sort().forEach((groupName) => {
            groups.push(groupByName[groupName])
          })

          // Create the index HTML file.
          const template = handlebars.compile(fs.readFileSync(path.join(__dirname, 'index.hbs'), 'utf8'), {
            preventIndent: true
          })

          fs.mkdirsSync(path.join(styleguide.path.build(), '_styleguide'))
          fs.writeFileSync(path.join(styleguide.path.build(), '_styleguide/index.html'), template({
            groups: groups
          }))

          // Create a project pointer for BE.
          fs.writeFileSync(
            path.join(styleguide.path.build(), '_name'),
            getProjectName())

          // Remove all unnecessary files.
          const packageDir = path.join(styleguide.path.build(), 'node_modules/*')

          del.sync([
            path.join(packageDir, '**/_theme.json')
          ])

          done()
        })
    },

    // Convert LESS files into CSS to be used by the styleguide UI itself.
    less: () => {
      return gulp.src(path.join(__dirname, 'index.less'))
        .pipe(less())
        .pipe(gulp.dest(path.join(styleguide.path.build(), '_styleguide')))
    },
    js: done => {
      let builder = new Builder()

      builder.config({
        defaultJSExtensions: true,
        baseURL: path.join(__dirname, '../../'),
        map: {
          'bliss': 'node_modules/blissfuljs/bliss.min.js'
        }
      })

      let buildOptions = {
        minify: false
      }

      builder.buildStatic(path.join(__dirname, 'index.js'), buildOptions).then(output => {
        gulp.src([ ])
          .pipe(plugins.file('index.js', output.source))
          .pipe(gulp.dest(path.join(styleguide.path.build(), '_styleguide')))
          .pipe(plugins.sourcemaps.init())
          .pipe(plugins.uglify())
          .pipe(plugins.rename({ extname: '.min.js' }))
          .pipe(plugins.sourcemaps.write('.'))
          .pipe(gulp.dest(path.join(styleguide.path.build(), '_styleguide')))
          .on('end', done)
      })
    }

  }

  gulp.task(styleguide.task.ui(), [ styleguide.task.clean() ], done => {
    styleguide.ui.copy(() => {
      styleguide.ui.html(() => {
        styleguide.ui.fonts().on('end', () => {
          styleguide.ui.js(() => {
            styleguide.ui.less().on('end', () => {
              done()
            })
          })
        })
      })
    })
  })
}
