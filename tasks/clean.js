const del = require('del');

module.exports = function (src) {
    if (!src) {
        src = ['_dist/**/*', '_build/**/*'];
    }
    return del(src);
}
