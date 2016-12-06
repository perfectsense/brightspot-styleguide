const fs = require('fs')
const glob = require('glob')
const less = require('gulp-less')
const gutil = require('gulp-util')
const handlebars = require('handlebars')
const path = require('path')
const through = require('through2')

const example = require('../example')
const label = require('../label')

module.exports = {
  registerModule: (styleguide) => {
    const gulp = styleguide._gulp
    const task = styleguide.task

    // Build fonts used by the styleguide UI itself.
    gulp.task(task.build.ui.fonts(), [ ], () => {
      return gulp.src(path.join(path.dirname(require.resolve('font-awesome/package.json')), 'fonts', '*'))
        .pipe(gulp.dest(path.join(styleguide.path.build(), '_styleguide')))
    })

    // Build LESS files used by the styleguide UI itself.
    gulp.task(task.build.ui.less(), [ ], () => {
      return gulp.src(path.join(__dirname, '..', 'styleguide.less'))
        .pipe(less())
        .pipe(gulp.dest(path.join(styleguide.path.build(), '_styleguide')))
    })

    // Build all files used by the styleguide UI itself.
    gulp.task(task.build.ui.all(), [
      task.build.ui.fonts(),
      task.build.ui.less()

    ], () => {
    })

    // Build all example HTML files.
    gulp.task(task.build.examples(), [ styleguide.task.copy.all() ], () => {
      function jsonToHtml(file, encoding, callback) {
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

            glob.sync(path.join(source, '**/*.json'), { absolute: true }).forEach(match => {
              const matchName = path.basename(match)

              if (matchName !== 'package.json' && matchName.slice(0, 1) !== '_') {
                const matchPath = path.join(fileDir, path.relative(source, match))

                this.push(new gutil.File({
                  base: styleguide.path.build(),
                  contents: new Buffer(example(styleguide.config, match)),
                  path: gutil.replaceExtension(matchPath, '.html')
                }))
              }
            })
          }

        } else if (fileName !== 'package.json' && fileName.slice(0, 1) !== '_') {
          file.base = styleguide.path.build()
          file.contents = new Buffer(example(styleguide.config, filePath))
          file.path = gutil.replaceExtension(filePath, '.html')
          this.push(file)
        }

        callback()
      }

      return gulp.src(path.join(styleguide.path.build(), 'styleguide/**/*.json'))
        .pipe(through.obj(jsonToHtml))
        .pipe(gulp.dest(styleguide.path.build()))
    })

    // Build the main styleguide HTML file.
    gulp.task(task.build.html(), [ task.build.examples() ], (done) => {
      glob('styleguide/**/*.html', { cwd: styleguide.path.build() }, (error, matches) => {
        const groupByName = { }

        // Group matches by their path.
        matches.forEach(match => {
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
            url: '/' + gutil.replaceExtension(match, '.html')
          })
        })

        // Sort the grouping so that the display is alphabetical.
        const groups = [ ]

        Object.keys(groupByName).sort().forEach((groupName) => {
          groups.push(groupByName[groupName])
        })

        const template = handlebars.compile(fs.readFileSync(path.join(__dirname, '..', 'styleguide.hbs'), 'utf8'), {
          preventIndent: true
        })

        fs.writeFileSync(path.join(styleguide.path.build(), '_styleguide/index.html'), template({
          groups: groups
        }))

        done()
      })
    })

    // Build all files.
    gulp.task(task.build.all(), [
      task.lint.all(),
      task.build.examples(),
      task.build.html(),
      task.build.project(),
      task.build.ui.all()

    ], () => {
    })
  }
}
