const Builder   = require('systemjs-builder')
const less      = require('gulp-less');

module.exports.styles = function (path) {
    if (!path) {
        path = [ '_build' ];
    }
    return less({
      paths: path
    })
}

module.exports.scripts = function () {
    var builder = new Builder()
}
