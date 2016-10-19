const less = require('gulp-less');
const SystemjsBuilder = require('gulp-systemjs-builder')
//const systemjsConfig = require('./systemjs.config.js')

module.exports.styles = function (path) {
    if (!path) {
        path = [ '_build' ];
    }
    return less({
      paths: path
    })
}

module.exports.scripts = function (config) {
    // Config API: https://github.com/systemjs/systemjs/blob/master/docs/config-api.md
    var builder = new SystemjsBuilder('_build/', {
        minify: false,
        mangle: true,
        runtime: false,
        defaultJSExtensions: true,
        globalDefs: { DEBUG: false, ENV: 'production' }
    })

    return builder.buildStatic('All.js', 'All.min.js')
}
