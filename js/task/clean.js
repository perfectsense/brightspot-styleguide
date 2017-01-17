const del = require('del')

module.exports = (styleguide, gulp) => {
  styleguide.task.clean = () => 'styleguide:clean'

  styleguide.clean = {
    all: () => {
      return del.sync([ `${styleguide.path.build()}/**/*` ])
    }
  }

  gulp.task(styleguide.task.clean(), (done) => {
    styleguide.clean.all()
    done()
  })
}
