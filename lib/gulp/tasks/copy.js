const path = require('path')
const extensionPattern = '{hbs,json}'
const filter = require('gulp-filter')
// const debug = require('gulp-debug')

module.exports = {
  registerModule: (styleguide) => {
    // Copies the template and JSON files into the target/snapshot and the target/gulp for the viewgenerator
    styleguide.gulp.task('copy:templates', ['copy:node_modules'], () => {
      return styleguide.gulp.src(styleguide.path.src(`**/*.${extensionPattern}`, {base: '.'}))
        .pipe(styleguide.gulp.dest(path.join(styleguide.path.build(), '..', 'gulp/styleguide')))
        .pipe(filter('**/*.hbs'))
        .pipe(styleguide.gulp.dest(path.join(styleguide.path.build(), 'styleguide')))
    })

    // Copies the template and JSON files from node_modules that contain a `styleguide` directory
    // into the target/snapshot and the target/gulp for the viewgenerator
    styleguide.gulp.task('copy:node_modules', () => {
      return styleguide.gulp.src([
        '!' + styleguide.path.src(`../node_modules/brightspot-styleguide/styleguide/**/*.${extensionPattern}`), // exclude brightspot-styleguide templates
        styleguide.path.src(`../node_modules/*/styleguide/**/*.${extensionPattern}`, {base: '.'})])
        .pipe(styleguide.gulp.dest(path.join(styleguide.path.build(), '..', 'gulp/node_modules')))
        .pipe(filter('**/*.hbs'))
        .pipe(styleguide.gulp.dest(path.join(styleguide.path.build(), 'node_modules')))
    })
  }
}
