const bower = require('gulp-bower');

module.exports = function (gulp) {
    return bower({ directory: './_tmp/bower_components', cwd: '.' })
}
