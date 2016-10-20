const paths = require('./paths.js');
const del = require('del');

module.exports.clean = function (src) {
    if (!src) {
        src = [paths.distRoot + '/**/*', paths.buildRoot + '/**/*'];
    }

    return del(src);
}
