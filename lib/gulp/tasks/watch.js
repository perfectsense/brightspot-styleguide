const Logger = require('../../logger')

module.exports = {
  registerModule: (styleguide) => {
    styleguide.watch = function () {
      this.gulp.watch([this.path.src('**/*.less'), `!${this.path.build()}**/*`], [ 'css' ]).on('change', (file) => { Logger.success(`Changed: ${file.path}`) })
      this.gulp.watch([this.path.src('**/*.js'), `!${this.path.build()}**/*`], [ 'js' ]).on('change', (file) => { Logger.success(`Changed: ${file.path}`) })
      this.gulp.watch([this.path.src('**/*.json'), `!${this.path.build()}**/*`], [ this.task.lint.json() ]).on('change', (file) => { Logger.success(`Changed: ${file.path}`) })
    }
  }
}
