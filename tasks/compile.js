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

module.exports.scripts = function (config) {
    var defaultConfig = 'systemjs.config.js'
    var builderConfig = config || defaultConfig

    var builder = new Builder()
    builder.loadConfig(builderConfig)
        .then(function() {
            console.log('loaded config');
            // ready to build
        });
}
