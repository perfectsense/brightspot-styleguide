const fs = require('fs')
const path = require('path')
const extensionPattern = '{hbs,json}'
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
    // Copies the template and JSON files into target/gulp for the viewgenerator
    styleguide._gulp.task('bsp-styleguide:copy:templates', ['bsp-styleguide:copy:createPackageFile'], () => {
      return styleguide._gulp.src(path.join(styleguide.config['project-src-path'], `**/*.${extensionPattern}`), { base: '.' })
        .pipe(styleguide._gulp.dest(styleguide.path.build()))
    })

    // creates a package.json at the root of each node_module within target/gulp
    styleguide._gulp.task('bsp-styleguide:copy:createPackageFile', ['bsp-styleguide:copy:node_modules'], () => {
      let folders = getFolders(path.join(styleguide.path.build(), 'node_modules'))

      folders.map((folder) => {
        return styleguide._gulp.src(path.join(styleguide.path.build(), 'node_modules', folder, '/*'))
                .pipe(file('package.json', '{}'))
                .pipe(styleguide._gulp.dest(path.join(styleguide.path.build(), 'node_modules', folder, '/')))
      })
    })

    // Copies the template and JSON files from node_modules that contain a `styleguide` directory
    // into the target/gulp directory for the viewgenerator
    styleguide._gulp.task('bsp-styleguide:copy:node_modules', () => {
      return styleguide._gulp.src([
        `!node_modules/brightspot-styleguide/styleguide/**/*.${extensionPattern}`, // exclude brightspot-styleguide templates
        `node_modules/*/styleguide/**/*.${extensionPattern}`
      ], { base: '.' })
        .pipe(styleguide._gulp.dest(styleguide.path.build()))
    })
  }
}
