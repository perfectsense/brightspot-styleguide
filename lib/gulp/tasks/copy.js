const path = require('path')

module.exports = {
  registerModule: (styleguide) => {
    styleguide.gulp.task('copy:templates', ['copy:node_modules'], () => {
      return styleguide.gulp.src(styleguide.path.src('**/*.hbs', {base: '.'}))
      .pipe(styleguide.gulp.dest(path.join(styleguide.path.build(), 'styleguide')))
    })

    styleguide.gulp.task('copy:node_modules', () => {
      return styleguide.gulp.src([
        '!' + styleguide.path.src('../node_modules/brightspot-styleguide/styleguide/**/*.hbs'), // exclude brightspot-styleguide templates
        styleguide.path.src('../node_modules/*/styleguide/**/*.hbs', {base: '.'})
      ])
      .pipe(styleguide.gulp.dest(path.join(styleguide.path.build(), 'node_modules')))
    })
  }
}
