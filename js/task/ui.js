const Builder = require('systemjs-builder')
const del = require('del')
const filter = require('gulp-filter')
const fs = require('fs-extra')
const glob = require('glob')
const gutil = require('gulp-util')
const handlebars = require('handlebars')
const less = require('gulp-less')
const path = require('path')
const plugins = require('gulp-load-plugins')()
const through = require('through2')
const traverse = require('traverse')
const xml2js = require('xml2js')
const zip = require('gulp-zip')

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
        path.join(styleguide.path.root(), 'styleguide/**/*.{hbs,json,md}')
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
                    fs.writeFileSync(styledExamplePath, JSON.stringify(exampleJson, null, '\t'))
                  })
                }

                // Create the template that delegates to the styled ones.
                styledTemplates[styledTemplate] = ''

                function appendStyledTemplate (template) {
                  const templatePath = resolver.path(rootPath, configPath, template.template)

                  styledTemplates[styledTemplate] += `{{#withParentPath '${path.relative(styleguide.path.build(), templatePath)}'}}`
                  styledTemplates[styledTemplate] += fs.readFileSync(templatePath, 'utf8')
                  styledTemplates[styledTemplate] += '{{/withParentPath}}'
                }

                for (let i = templates.length - 1; i > 0; --i) {
                  const template = templates[i]
                  const internalName = template.internalName || template.template

                  styledTemplates[styledTemplate] += `{{#styledTemplate '${internalName}'}}`
                  appendStyledTemplate(template)
                  styledTemplates[styledTemplate] += '{{else}}'
                }

                appendStyledTemplate(templates[0])

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
                      fs.writeFileSync(themeExamplePath, JSON.stringify(exampleJson, null, '\t'))
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
      const imageSizes = { }

      function addImageSize (template, field, imageSize) {
        if (!template) return
        let x = imageSizes[template]
        if (!x) x = imageSizes[template] = { }
        let y = x[field]
        if (!y) y = imageSizes[template][field] = [ ]
        y.push(imageSize)
      }

      const originalTemplates = { }
      const styledTemplates = { }
      const configPath = path.join(getProjectRootPath(), 'styleguide/_config.json')

      if (fs.existsSync(configPath)) {
        const styles = JSON.parse(fs.readFileSync(configPath, 'utf8')).styles

        if (styles) {
          const rootPath = styleguide.path.root()

          Object.keys(styles).forEach(originalTemplate => {
            const relativeOriginalTemplate = '/' + path.relative(styleguide.path.root(), resolver.path(rootPath, configPath, originalTemplate))

            styles[originalTemplate].templates.forEach(template => {
              const relativeTemplate = '/' + path.relative(styleguide.path.build(), resolver.path(rootPath, configPath, template.template))

              originalTemplates[relativeTemplate] = relativeOriginalTemplate
              styledTemplates[relativeOriginalTemplate] = styledTemplates[relativeOriginalTemplate] || [ ]
              styledTemplates[relativeOriginalTemplate].push(relativeTemplate)
            })
          })
        }
      }

      function jsonToHtml (file, encoding, callback) {
        const filePath = file.path
        const fileName = path.basename(filePath)

        if (fileName !== 'package.json' && fileName.slice(0, 1) !== '_') {
          try {
            const processedExample = example(styleguide, filePath)

            if (processedExample) {
              traverse(processedExample.data).forEach(function (value) {
                if (typeof value === 'string') {
                  const match = value.match(/\{\{\s*image\s*\(\s*(\d+|\[[^]]+])\s*,\s*(\d+|\[[^]]+])\s*\)\s*}}/)

                  if (match) {
                    const selector = [ ]

                    for (let parent = this.parent; parent; parent = parent.parent) {
                      const template = parent.node._template

                      if (template) {
                        const relativeTemplate = '/' + path.relative(styleguide.path.build(), template)
                        const originalTemplate = originalTemplates[relativeTemplate]
                        const index = parseInt(parent.key, 10)

                        selector.unshift(relativeTemplate)

                        if (originalTemplate) {
                          selector.unshift(originalTemplate)
                        }

                        if (!isNaN(index)) {
                          const grandparent = parent.parent
                          const grandparentTemplate = grandparent.parent.node._template

                          if (grandparentTemplate) {
                            const grandparentKey = '/' + path.relative(styleguide.path.build(), grandparentTemplate) + ':' + grandparent.key

                            selector.unshift(grandparentKey + ':' + index)
                            selector.unshift(grandparentKey)
                          }
                        }
                      }
                    }

                    const template = selector[selector.length - 1]
                    const field = this.key
                    const imageSize = {
                      selector: selector,
                      width: parseInt(match[1], 10),
                      height: parseInt(match[2], 10)
                    }

                    addImageSize(template, field, imageSize)
                    addImageSize(originalTemplates[template], field, imageSize)
                  }
                }
              })

              file.base = styleguide.path.build()
              file.contents = Buffer.from(processedExample.html)
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

      gulp.src(['!' + path.join(projectRootPath, 'styleguide/_sketch/**/*'), path.join(projectRootPath, 'styleguide/**/*.json')])
        .pipe(through.obj(jsonToHtml))
        .pipe(gulp.dest(styleguide.path.build()))
        .on('end', () => {
          // Group example HTML files by their path.
          const groupByName = { }

          glob.sync('**/*.html', { cwd: styleguide.path.build() }).forEach(match => {
            const groupName = path.dirname(path.relative(path.join(projectRootPath, 'styleguide'), path.join(styleguide.path.build(), match))).split('/').map(label).join(': ')
            let group = groupByName[groupName]
            let item = {}
            item.name = label(path.basename(match, '.html'))
            item.url = '/' + gutil.replaceExtension(match, '.html')
            item.source = {'html': 'Example', 'json': 'JSON'}

            if (!group) {
              group = groupByName[groupName] = {
                name: groupName,
                examples: [ ]
              }
            }

            if (fs.existsSync(gutil.replaceExtension(path.join(styleguide.path.build(), match), '.md'))) {
              item.source = Object.assign(item.source, {'md': 'Documentation'})
            }

            group.examples.push(item)
          })

          // Sort the grouping so that the display is alphabetical.
          const groups = [ ]

          Object.keys(groupByName).sort().forEach((groupName) => {
            groups.push(groupByName[groupName])
          })

          const designElements = { }
          const lesshintOverrides = `hexNotation: false, spaceAroundComma: false, maxCharPerLine: false, singleLinePerProperty: false, newlineAfterBlock: false`

          glob.sync('**/*.json', { cwd: path.join(styleguide.path.root(), `styleguide/_sketch/`), absolute: true }).forEach(sketchFile => {
            const lessFilename = sketchFile.replace(path.extname(sketchFile), '.less')
            let styles = JSON.parse(fs.readFileSync(sketchFile, 'utf8')).styles
            let lessData = `// lesshint ${lesshintOverrides}\n/* This file was autogenerated by the Brightspot Express Sketch plugin. DO NOT edit this file directly. */\n\n`

            // Process only the "colorStyles".
            let colorStyles = styles.filter(style => { return (style._type === `colorStyle`) })
            colorStyles = colorStyles.sort((a, b) => a.name > b.name)

            // Generate Less color mixins.
            colorStyles.forEach(colorStyle => {
              if (!colorStyle.color) return
              let cssProperty = ''

              colorStyle.lessMixin = `.sketch("${colorStyle.name}")`

              if (colorStyle.color.stops && colorStyle.color.stops.length) {
                colorStyle.color.stops.forEach(stop => {
                  cssProperty += `rgba(${stop.red}, ${stop.green}, ${stop.blue}, ${stop.alpha}) ${stop.position}%,`
                })
              } else {
                cssProperty += `rgb(${colorStyle.color.red}, ${colorStyle.color.green}, ${colorStyle.color.blue});\n`
              }

              return cssProperty
            })

            // Process only the "textStyles".
            let textStyles = styles.filter(style => { return (style._type === `textStyle`) })
            textStyles = textStyles.sort((a, b) => a.name > b.name)

            // Get the unique font-families.
            const fontFamilies = [ ...new Set(textStyles.map(style => style.fontFamily)) ].sort()

            textStyles.forEach(textStyle => {
              // Based on the luminance of the color,
              // determine which contrasting shade to use.
              // credit: https://24ways.org/2010/calculating-color-contrast/
              if (textStyle.color) {
                const yiq = ((textStyle.color.red * 299) + (textStyle.color.green * 587) + (textStyle.color.blue * 114)) / 1000
                const contrast = (yiq >= 128) ? 'dark' : 'light'
                textStyle.contrast = contrast
              }

              if (textStyle.alignment) {
                textStyle.cssProps += `text-align: ${textStyle.alignment};`
              }

              // Format the CSS ruleset.
              let cssProperties = textStyle.cssProps
                .split(';').map(prop => {
                  return `\n  ${prop.trim()}`
                }).join(';')

              // Generate the Less mixin.
              lessData += `\n.sketch(@textStyle) when (@textStyle = "${textStyle.name}") {\n  ${cssProperties.trim()}\n}\n`
            })

            designElements[`${path.parse(sketchFile).name}`] = {
              name: `${path.parse(sketchFile).name}`,
              fontFamilies: fontFamilies,
              textStyles: textStyles
            }

            try {
              fs.writeFileSync(lessFilename, lessData)
            } catch (e) {
              logger.error(`Error writing file: ${lessFilename}`)
            }
          })

          // Create the sketch template function.
          const sketchTemplate = handlebars.compile(fs.readFileSync(path.join(__dirname, '../', 'sketch.hbs'), 'utf8'), {
            preventIndent: true
          })

          // Create the iframed index HTML file.
          fs.writeFileSync(path.join(getProjectRootPath(), 'styleguide/index.html'), sketchTemplate({
            designElements: designElements
          }))

          // Create the index template function.
          const template = handlebars.compile(fs.readFileSync(path.join(__dirname, '../', 'index.hbs'), 'utf8'), {
            preventIndent: true
          })

          // Create the index HTML file for the styleguide itself.
          fs.mkdirsSync(path.join(styleguide.path.build(), '_styleguide'))
          fs.writeFileSync(path.join(styleguide.path.build(), '_styleguide/index.html'), template({
            indexUrl: path.join('/node_modules', getProjectName(), 'styleguide/index.html'),
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

          fs.writeFileSync(
            path.join(styleguide.path.build(), '_image-sizes'),

            JSON.stringify({
              originalTemplates: originalTemplates,
              styledTemplates: styledTemplates,
              imageSizes: imageSizes
            }, null, '  '))

          done()
        })
    },

    zip: done => {
      const pomFile = path.resolve(styleguide.path.root(), 'pom.xml')
      let name = getProjectName()

      if (fs.existsSync(pomFile)) {
        xml2js.parseString(fs.readFileSync(pomFile), { async: false }, (error, pomXml) => {
          if (error) {
            throw error
          }

          name = `${pomXml.project.artifactId}-${pomXml.project.version}`
        })
      }

      return gulp.src(`${styleguide.path.build()}/**`)
        .pipe(zip(`${name}.zip`))
        .pipe(gulp.dest(path.join(styleguide.path.build(), '..')))
        .on('end', done)
    },

    // Convert LESS files into CSS to be used by the styleguide UI itself.
    less: () => {
      return gulp.src(path.join(__dirname, '../', 'index.less'))
        .pipe(less())
        .pipe(gulp.dest(path.join(styleguide.path.build(), '_styleguide')))
    },

    // JavaScript transpilation to be used by the styleguide UI itself.
    js: done => {
      let builder = new Builder()
      const indexPath = require.resolve('../index')

      builder.config({
        defaultJSExtensions: true,
        baseURL: path.dirname(indexPath),
        paths: {
          'bliss': require.resolve('blissfuljs/bliss.min.js'),
          'prism': require.resolve('prismjs/prism.js'),
          'prism-previewer-base': require.resolve('prismjs/plugins/previewer-base/prism-previewer-base.js'),
          'prism-previewer-color': require.resolve('prismjs/plugins/previewer-color/prism-previewer-color.js'),
          'prism-json': require.resolve('prismjs/components/prism-json.min.js'),
          'prism-markdown': require.resolve('prismjs/components/prism-markdown.min.js')
        }
      })

      let buildOptions = {
        minify: false
      }

      builder.buildStatic(indexPath, buildOptions).then(output => {
        gulp.src([ ])
          .pipe(plugins.file('index.js', output.source))
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
              if (!styleguide.isWatching()) {
                styleguide.ui.zip(done)
              } else {
                done()
              }
            })
          })
        })
      })
    })
  })
}
