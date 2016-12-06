const fs = require('fs-extra')
const path = require('path')
const extensionPattern = '{hbs,json}'
const file = require('gulp-file')
const filter = require('gulp-filter')
const handlebars = require('handlebars')
const less = require('gulp-less')
const gutil = require('gulp-util')
const glob = require('glob')
const sentenceCase = require('sentence-case')
const through = require('through2')

module.exports = {
  registerModule: (styleguide) => {
    let gulp = styleguide._gulp

    gulp.task('styleguide:build:server:fonts', [ ], () => {
      return gulp.src(path.join(path.dirname(require.resolve('font-awesome/package.json')), 'fonts', '*'))
              .pipe(gulp.dest(styleguide.path.build('fonts')))
    })

    gulp.task('styleguide:build:server:css', [], () => {
      return gulp.src(path.join(__dirname, '..', '..', 'styleguide.less'))
              .pipe(less())
              .pipe(gulp.dest(styleguide.path.build()))
    })

    gulp.task('styleguide:build:server', [ 'styleguide:build:server:fonts', 'styleguide:build:server:css' ], () => {
    })

    gulp.task('styleguide:build:html', [ 'styleguide:postcopy:templates' ], () => {
      return gulp.src(path.join(styleguide.path.build(), '**/*.json'))
        .pipe(through.obj((file, encoding, callback) => {
          if (!fs.statSync(file.path).isDirectory() && path.basename(file.path).slice(0, 1) !== '_') {
            file.contents = new Buffer(require('../../example-file')(styleguide.config, file.path))
            file.path = gutil.replaceExtension(file.path, '.html')
          }
          callback(null, file)
        }))
        .pipe(gulp.dest(styleguide.path.build()))
    })

    gulp.task('styleguide:build:index', [ 'styleguide:build:html' ], (done) => {
      glob('**/*.json', { cwd: styleguide.path.build(), ignore: '**/_*.json' }, (errors, files) => {
        const groupByName = { }

        files.forEach((file) => {
          const groupName = path.dirname(file).split('/').map(sentenceCase).join(': ')
          let group = groupByName[groupName]

          if (!group) {
            group = groupByName[groupName] = {
              name: groupName,
              children: [ ]
            }
          }

          group.children.push({
            name: sentenceCase(path.basename(file, '.json')),
            url: '/' + gutil.replaceExtension(file, '.html')
          })
        })

        const groups = [ ]

        Object.keys(groupByName).sort().forEach((groupName) => {
          groups.push(groupByName[groupName])
        });

        const template = handlebars.compile(fs.readFileSync(path.join(__dirname, '..', '..', 'styleguide.hbs'), 'utf8'), {
          preventIndent: true
        })

        fs.writeFileSync(path.join(styleguide.path.build(), 'styleguide.html'), template({
          groups: groups
        }))

        done()
      })
    })

    gulp.task('styleguide:build', [ 'styleguide:build:index', 'styleguide:build:server' ], () => {
    })
  }
}
