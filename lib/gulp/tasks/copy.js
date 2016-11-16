const fs = require('fs')
const path = require('path')
const extensionPattern = '{hbs,json}'
const filter = require('gulp-filter')
const file = require('gulp-file')

function getFolders (dir) {
  if (fs.existsSync(dir)) {
    return fs.readdirSync(dir)
      .filter((file) => {
        return fs.statSync(path.join(dir, file)).isDirectory()
      })
  }
  return []
}

module.exports = {
  registerModule: (styleguide) => {
    // Copies the template and JSON files into the target/snapshot and the target/gulp for the viewgenerator
    styleguide.gulp.task('copy:templates', ['copy:createPackageFile'], () => {
      return styleguide.gulp.src(styleguide.path.src(`**/*.${extensionPattern}`, {base: '.'}))
        .pipe(styleguide.gulp.dest(path.join(styleguide.path.build(), '..', 'gulp/styleguide')))
        .pipe(filter('**/*.hbs'))
        .pipe(styleguide.gulp.dest(path.join(styleguide.path.build(), 'styleguide')))
    })

    // creates a package.json at the root of each node_module within target/gulp
    styleguide.gulp.task('copy:createPackageFile', ['copy:node_modules'], () => {
      let folders = getFolders(path.join(styleguide.path.build(), '..', 'gulp/node_modules'))

      folders.map((folder) => {
        return styleguide.gulp.src(path.join(styleguide.path.build(), '..', 'gulp/node_modules', folder, '/*'))
                .pipe(file('package.json', '{}'))
                .pipe(styleguide.gulp.dest(path.join(styleguide.path.build(), '..', 'gulp/node_modules', folder, '/')))
      })
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
