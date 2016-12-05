const fs = require('fs-extra')
const path = require('path')
const extensionPattern = '{hbs,json}'
const file = require('gulp-file')
const filter = require('gulp-filter')
const less = require('gulp-less')
const glob = require('glob')

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
    const gulp = styleguide._gulp

    function config () {
      return filter((file) => {
        let config = require(file.path)
        let reportData = {
          overrides: [ ],
          errors: [ ]
        }

        // process 'source` directives
        if (`source` in config) {
          let relativePath
          let sourcePath
          let basePath = path.dirname(file.path)

          // determine where each template should be moved and update the report data
          glob.sync(`${basePath}/**/*.hbs`, { }).forEach((filePath) => {
            relativePath = filePath.split(basePath)[1]
            sourcePath = path.resolve(path.join(styleguide.path.build(), config.source, relativePath))
            if (fs.existsSync(sourcePath)) {
              reportData.overrides.push({
                sourceOrigin: sourcePath,
                sourceDest: path.resolve(sourcePath, '..', '_source', path.basename(sourcePath)),
                overrideOrigin: filePath,
                overrideDest: sourcePath
              })
            } else {
              reportData.errors.push({
                message: `File "${filePath}" cannot source from "${sourcePath}" because that source path doesn't exist.`
              })
            }
          })

          // move files.
          if (reportData.overrides.length > 0) {
            reportData.overrides.forEach((file) => {
              // move source files first...
              fs.move(file.sourceOrigin, file.sourceDest, { clobber: true }, (err) => {
                if (err) {
                  console.log(err)
                } else {
                  // ...then overrides
                  fs.move(file.overrideOrigin, file.overrideDest, { clobber: true }, (err) => {
                    if (err) {
                      console.log(err)
                    }
                  })
                }
              })
            })
          }

          // generate a report file
          fs.writeFileSync(path.join(styleguide.path.build(), '_styleguide-report.json'), JSON.stringify(reportData, null, `\t`))
        }

        // throw if any errors were detected
        if (reportData.errors.length > 0) {
          throw new Error(reportData.errors.map((error) => {
            return `\n${error.message}\n`
          }))
        }
      })
    }

    // Looks for _config.json directives within the target because we might need to post-process something in the build dir
    gulp.task('styleguide:postcopy:templates', ['styleguide:copy:templates'], () => {
      return gulp.src([`${styleguide.path.build()}/**/*/_config.json`, `!${styleguide.path.build()}/node_modules/**/*`])
        .pipe(config())
    })

    // Copies the template and JSON files into target/gulp for the viewgenerator
    gulp.task('styleguide:copy:templates', ['styleguide:copy:createPackageFile'], () => {
      return gulp.src(path.join(styleguide.config.source, `**/*.${extensionPattern}`), { base: '.' })
        .pipe(gulp.dest(styleguide.path.build()))
    })

    // creates a package.json at the root of each node_module within target/gulp
    gulp.task('styleguide:copy:createPackageFile', ['styleguide:copy:node_modules'], () => {
      let folders = getFolders(path.join(styleguide.path.build(), 'node_modules'))

      folders.map((folder) => {
        return gulp.src(path.join(styleguide.path.build(), 'node_modules', folder, '/*'))
                .pipe(file('package.json', '{}'))
                .pipe(gulp.dest(path.join(styleguide.path.build(), 'node_modules', folder, '/')))
      })
    })

    // Copies the template and JSON files from node_modules that contain a `styleguide` directory
    // into the target/gulp directory for the viewgenerator
    gulp.task('styleguide:copy:node_modules', [], () => {
      return gulp.src([
        `!node_modules/brightspot-styleguide/styleguide/**/*.${extensionPattern}`, // exclude brightspot-styleguide templates
        `node_modules/*/styleguide/**/*.${extensionPattern}`
      ], { base: '.' })
        .pipe(gulp.dest(styleguide.path.build()))
    })

    gulp.task('styleguide:copy:json', [], () => {
      return gulp.src(path.join(styleguide.config.source, `**/*.json`), { base: '.' })
        .pipe(gulp.dest(styleguide.path.build()))
    })
  }
}
