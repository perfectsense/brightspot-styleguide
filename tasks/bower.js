const bower = require('gulp-bower');

module.exports.bower = function () {
    return bower({ directory: './_build/bower_components', cwd: '.' })
}
