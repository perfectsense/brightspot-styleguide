const Logger = require('../../logger')
const path = require('path')

module.exports = {
  registerModule: (styleguide) => {
    styleguide._gulp.task('styleguide:watch', () => {
      styleguide._gulp.watch([path.join(styleguide.config['project-src-path'], '**/*.hbs'), `!${styleguide.path.build()}**/*`], [ styleguide.task.copy.templates() ])
        .on('change', (file) => { Logger.success(`Changed: ${file.path}`) })
      styleguide._gulp.watch([path.join(styleguide.config['project-src-path'], '**/*.less'), `!${styleguide.path.build()}**/*`], [ 'css' ])
        .on('change', (file) => { Logger.success(`Changed: ${file.path}`) })
      styleguide._gulp.watch([path.join(styleguide.config['project-src-path'], '**/*.js'), `!${styleguide.path.build()}**/*`], [ 'js' ])
        .on('change', (file) => { Logger.success(`Changed: ${file.path}`) })
      styleguide._gulp.watch([path.join(styleguide.config['project-src-path'], '**/*.json'), `!${styleguide.path.build()}**/*`], [ styleguide.task.lint.json(), styleguide.task.copy.json() ])
        .on('change', (file) => { Logger.success(`Changed: ${file.path}`) })
    })
  }
}
