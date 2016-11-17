const Logger = require('../../logger')

module.exports = {
  registerModule: (styleguide) => {
    styleguide.gulp.task('bsp-styleguide:watch', () => {
      styleguide.gulp.watch([styleguide.path.src('**/*.less'), `!${styleguide.path.build()}**/*`], [ 'css' ]).on('change', (file) => { Logger.success(`Changed: ${file.path}`) })
      styleguide.gulp.watch([styleguide.path.src('**/*.js'), `!${styleguide.path.build()}**/*`], [ 'js' ]).on('change', (file) => { Logger.success(`Changed: ${file.path}`) })
      styleguide.gulp.watch([styleguide.path.src('**/*.json'), `!${styleguide.path.build()}**/*`], [ styleguide.task.lint.json() ]).on('change', (file) => { Logger.success(`Changed: ${file.path}`) })
    })
  }
}
