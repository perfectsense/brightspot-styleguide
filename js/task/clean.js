const del = require('del')

module.exports = (styleguide, gulp) => {
  styleguide.task.clean = () => 'styleguide:clean'

  styleguide.clean = {
    all: () => {
      if (styleguide.failOnErrors) {
        del.sync([ `${styleguide.path.build()}/**/*` ])
      }
    }
  }

  gulp.task(styleguide.task.clean(), (done) => {
    styleguide.clean.all()
    done()
  })
}
