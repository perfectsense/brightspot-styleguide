const filter = require('gulp-filter')
const fs = require('fs-extra')
const glob = require('glob')
const less = require('gulp-less')
const gutil = require('gulp-util')
const handlebars = require('handlebars')
const path = require('path')
const through = require('through2')
const traverse = require('traverse')

const example = require('../example')
const label = require('../label')
const resolver = require('../resolver')

module.exports = (styleguide, gulp) => {
  styleguide.task.ui = () => 'styleguide:ui'

  styleguide.ui = {

    // Copy all files related to producing example HTML.
    copy: done => {
      const projectFiles = [
        path.join(styleguide.path.root(), 'package.json'),
        path.join(styleguide.path.source(), '**/*.{hbs,json}'),
        path.join(styleguide.path.root(), 'node_modules/*/package.json'),
        path.join(styleguide.path.root(), 'node_modules/*/styleguide/**/*.{hbs,json}')
      ]

      // Automatically create all files related to styled templates.
      const configPath = path.join(styleguide.path.source(), '_config.json')
      const styledTemplates = { }

      if (fs.existsSync(configPath)) {
        const styles = JSON.parse(fs.readFileSync(configPath, 'utf8')).styles

        if (styles) {
          Object.keys(styles).forEach(styledTemplate => {
            const style = styles[styledTemplate]
            const templates = style.templates

            // Create styled example JSON files.
            if (templates) {
              templates.forEach(template => {
                const example = template.example || style.example
                const examplePath = path.join(styleguide.path.root(), example)
                const exampleJson = JSON.parse(fs.readFileSync(examplePath, 'utf8'))

                traverse(exampleJson).forEach(function (value) {
                  if (!value) {
                    return
                  }

                  if (this.key === '_template' &&
                    resolver.path(styleguide.path.root(), examplePath, value) === resolver.path(styleguide.path.root(), examplePath, styledTemplate)) {
                    this.update(template.template)
                  } else if (this.key === '_template' ||
                    this.key === '_wrapper' ||
                    this.key === '_include' ||
                    this.key === '_dataUrl') {
                    this.update(path.join(path.dirname(example), value))
                  }
                })

                const styledExamplePath = gutil.replaceExtension(path.join(styleguide.path.build(), template.template), '.json')

                fs.mkdirsSync(path.dirname(styledExamplePath))
                fs.writeFileSync(styledExamplePath, JSON.stringify(exampleJson))
              })
            }

            // Create the template that delegates to the styled ones.
            styledTemplates[styledTemplate] = ''

            for (let i = templates.length - 1; i > 0; --i) {
              const template = templates[i]
              const internalName = template.internalName || template.template
              const templatePath = path.join(styleguide.path.root(), template.template)

              styledTemplates[styledTemplate] += `{{#if _template.[${internalName}]}}${fs.readFileSync(templatePath, 'utf8')}{{else}}`
            }

            styledTemplates[styledTemplate] += fs.readFileSync(path.join(styleguide.path.root(), templates[0].template), 'utf8')

            for (let i = templates.length - 1; i > 0; --i) {
              styledTemplates[styledTemplate] += '{{/if}}'
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

      gulp.src(projectFiles, { base: '.' })
        .pipe(onlyStyleguidePackages)
        .pipe(gulp.dest(styleguide.path.build()))

        // Copy all files related to theming.
        .on('end', () => {
          glob.sync(path.join(styleguide.path.build(), 'styleguide/**/_theme.json'), { absolute: true }).forEach(themeFile => {
            const theme = JSON.parse(fs.readFileSync(themeFile, 'utf8'))

            // Make sure source exists and is resolved.
            let source = theme.source
            if (!source) return
            source = path.join(styleguide.path.build(), source)

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

            // Generate a report of what's been done.
            fs.writeFileSync(path.join(themeDir, '_theme-report.json'), JSON.stringify(report, null, 4))

            // Throw any errors that were detected.
            if (report.errors.length > 0) {
              throw new Error(report.errors.map(error => `\n${error.message}\n`))
            }
          })

          // Override styled templates.
          Object.keys(styledTemplates).forEach(styledTemplate => {
            const styledTemplatePath = path.join(styleguide.path.build(), styledTemplate)

            fs.mkdirsSync(path.dirname(styledTemplatePath))
            fs.writeFileSync(styledTemplatePath, styledTemplates[styledTemplate])
          })

          done()
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

        // Theming something else?
        if (fileName === '_theme.json') {
          const theme = JSON.parse(fs.readFileSync(filePath, 'utf8'))
          let source = theme.source

          if (source && !theme.hidden) {
            source = source.slice(0, 1) === '/'
              ? path.join(styleguide.path.build(), source)
              : path.resolve(filePath, source)

            const fileDir = path.dirname(filePath)

            // Use the example JSON files in the source and put the produced
            // HTML into the theme directory.
            glob.sync(path.join(source, '**/*.json'), { absolute: true }).forEach(match => {
              const matchName = path.basename(match)

              if (matchName !== 'package.json' && matchName.slice(0, 1) !== '_') {
                const matchPath = path.join(fileDir, path.relative(source, match))

                this.push(new gutil.File({
                  base: styleguide.path.build(),
                  contents: new Buffer(example(styleguide, match)),
                  path: gutil.replaceExtension(matchPath, '.html')
                }))
              }
            })
          }
        } else if (fileName !== 'package.json' && fileName.slice(0, 1) !== '_') {
          file.base = styleguide.path.build()
          file.contents = new Buffer(example(styleguide, filePath))
          file.path = gutil.replaceExtension(filePath, '.html')
          this.push(file)
        }

        callback()
      }

      // Build the index HTML that serves as the entry to the styleguide UI
      // after all the example HTML files are produced.
      gulp.src(path.join(styleguide.path.build(), 'styleguide/**/*.json'))
        .pipe(through.obj(jsonToHtml))
        .pipe(gulp.dest(styleguide.path.build()))
        .on('end', () => {
          // Group example HTML files by their path.
          const groupByName = { }

          glob.sync('**/*.html', { cwd: path.join(styleguide.path.build(), 'styleguide') }).forEach(match => {
            const groupName = path.dirname(match).split('/').map(label).join(': ')
            let group = groupByName[groupName]

            if (!group) {
              group = groupByName[groupName] = {
                name: groupName,
                children: [ ]
              }
            }

            group.children.push({
              name: label(path.basename(match, '.html')),
              url: '/styleguide/' + gutil.replaceExtension(match, '.html')
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

          done()
        })
    },

    // Convert LESS files into CSS to be used by the styleguide UI itself.
    less: () => {
      return gulp.src(path.join(__dirname, '..', 'styleguide.less'))
        .pipe(less())
        .pipe(gulp.dest(path.join(styleguide.path.build(), '_styleguide')))
    }
  }

  gulp.task(styleguide.task.ui(), [ styleguide.task.clean() ], done => {
    styleguide.ui.copy(() => {
      styleguide.ui.html(() => {
        styleguide.ui.fonts().on('end', () => {
          styleguide.ui.less().on('end', () => {
            done()
          })
        })
      })
    })
  })
}
