const bower = require('gulp-bower');

module.exports = function () {
    return bower({ directory: './_tmp/bower_components', cwd: '.' })
}
