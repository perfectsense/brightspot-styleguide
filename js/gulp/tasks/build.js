const fs = require('fs-extra')
const path = require('path')
const extensionPattern = '{hbs,json}'
const file = require('gulp-file')
const filter = require('gulp-filter')
const less = require('gulp-less')
const gutil = require('gulp-util')
const glob = require('glob')
const through = require('through2')

module.exports = {
  registerModule: (styleguide) => {
    let gulp = styleguide._gulp;

    gulp.task('styleguide:build:server:fonts', [ ], () => {
      return gulp.src(path.join(path.dirname(require.resolve('font-awesome/package.json')), 'fonts', '*'))
              .pipe(gulp.dest(styleguide.path.build('fonts')))
    })

    gulp.task('styleguide:build:server:css', [], () => {
      return gulp.src(path.join(__dirname, '..', '..', '..', 'styleguide', 'styleguide.less'))
              .pipe(less())
              .pipe(gulp.dest(styleguide.path.build()))
    })

    gulp.task('styleguide:build:server', [ 'styleguide:build:server:fonts', 'styleguide:build:server:css' ], () => {
    })

    gulp.task('styleguide:build:html', [ 'styleguide:postcopy:templates' ], () => {
      console.log(styleguide.path.build('**/*.json'))
      return gulp.src(path.join(styleguide.path.build(), '**/*.json'))
          .pipe(through.obj((file, encoding, callback) => {
            console.log('path: ' + file.path);
            if (!fs.statSync(file.path).isDirectory() && path.basename(file.path).slice(0, 1) !== '_') {
              file.contents = new Buffer(require('../../example-file')(styleguide.config, file.path));
              file.path = gutil.replaceExtension(file.path, '.html')
            }
            callback(null, file)
          }))
          .pipe(gulp.dest(styleguide.path.build()))
    })

    gulp.task('styleguide:build', [ 'styleguide:build:html', 'styleguide:build:server' ], () => {
    })
  }
}
