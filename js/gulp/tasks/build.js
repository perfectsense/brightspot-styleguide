const fs = require('fs')
const glob = require('glob')
const less = require('gulp-less')
const gutil = require('gulp-util')
const handlebars = require('handlebars')
const path = require('path')
const through = require('through2')

const label = require('../../label')

module.exports = {
  registerModule: (styleguide) => {
    const gulp = styleguide._gulp

    // Build fonts used by the styleguide itself.
    gulp.task('styleguide:build:styleguide:fonts', [ ], () => {
      return gulp.src(path.join(path.dirname(require.resolve('font-awesome/package.json')), 'fonts', '*'))
        .pipe(gulp.dest(styleguide.path.build()))
    })

    // Build CSS used by the styleguide itself.
    gulp.task('styleguide:build:styleguide:css', [], () => {
      return gulp.src(path.join(__dirname, '..', '..', 'styleguide.less'))
        .pipe(less())
        .pipe(gulp.dest(styleguide.path.build()))
    })

    // Build all files used by the styleguide itself.
    gulp.task('styleguide:build:styleguide', [ 'styleguide:build:styleguide:fonts', 'styleguide:build:styleguide:css' ], () => {
    })

    // Build all example HTML files.
    gulp.task('styleguide:build:html', [ 'styleguide:postcopy:templates' ], () => {
      const build = styleguide.path.build()

      function jsonToHtml(file, encoding, callback) {
        const filePath = file.path
        const fileName = path.basename(filePath)

        // Source JSON files from elsewhere?
        if (fileName === '_config.json') {
          let source = JSON.parse(fs.readFileSync(filePath, 'utf8')).source

          if (source) {
            source = source.slice(0, 1) === '/'
              ? path.join(build, source)
              : path.resolve(filePath, source)

            const fileDir = path.dirname(filePath)

            glob.sync(path.join(source, '**/*.json'), { absolute: true }).forEach(match => {
              const matchName = path.basename(match)

              if (matchName !== 'package.json' && matchName.slice(0, 1) !== '_') {
                const matchPath = path.join(fileDir, path.relative(source, match))

                this.push(new gutil.File({
                  base: build,
                  contents: new Buffer(require('../../example-file')(styleguide.config, match)),
                  path: gutil.replaceExtension(matchPath, '.html')
                }))
              }
            })
          }

        } else if (fileName !== 'package.json' && fileName.slice(0, 1) !== '_') {
          file.base = build
          file.contents = new Buffer(require('../../example-file')(styleguide.config, filePath))
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
    gulp.task('styleguide:build:index', [ 'styleguide:build:html' ], (done) => {
      const build = styleguide.path.build()

      glob('styleguide/**/*.html', { cwd: build }, (error, matches) => {
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

        const template = handlebars.compile(fs.readFileSync(path.join(__dirname, '..', '..', 'styleguide.hbs'), 'utf8'), {
          preventIndent: true
        })

        fs.writeFileSync(path.join(build, 'styleguide.html'), template({
          groups: groups
        }))

        done()
      })
    })

    // Build all files.
    gulp.task('styleguide:build', [ 'styleguide:build:styleguide', 'styleguide:build:index' ], () => {
    })
  }
}
