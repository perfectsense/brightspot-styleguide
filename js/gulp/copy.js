const fs = require('fs-extra')
const path = require('path')
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
    const task = styleguide.task

    gulp.task(task.copy.html(), () => {
      return gulp.src([
        path.join(styleguide.config.source, '**/*.{hbs,json}'),
        path.join(styleguide.config.root, 'node_modules/*/styleguide/**/*.{hbs,json}')

      ], { base: '.' })
        .pipe(gulp.dest(styleguide.path.build()))
    })

    // Looks for _config.json directives within the target because we might need to post-process something in the build dir
    gulp.task(task.copy.sourced(), [ task.copy.html() ], () => {
      return gulp.src([`${styleguide.path.build()}/**/*/_config.json`, `!${styleguide.path.build()}/node_modules/**/*`])
        .pipe(filter((file) => {
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
                  sourceDest: path.resolve(sourcePath, '..', '_Source' + path.basename(sourcePath)),
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
        }))
    })

    gulp.task(task.copy.all(), [
      task.copy.html(),
      task.copy.sourced()

    ], () => {
    })
  }
}
