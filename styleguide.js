const paths = require('./gulp/paths.js')

var Styleguide = function (config) {
    this.config = config
}

Styleguide.prototype.distRoot = function () {
    return paths.distRoot
};

Styleguide.prototype.buildRoot = function () {
    return paths.buildRoot
};

Styleguide.prototype.clean = require('./gulp/clean.js').clean;
Styleguide.prototype.copySrc = require('./gulp/copy.js').copySrc;
Styleguide.prototype.bowerInstall = require('./gulp/bower.js').bowerInstall;
Styleguide.prototype.copyBower = require('./gulp/bower.js').copyBower;
Styleguide.prototype.compileScripts = require('./gulp/compile.js').scripts;
Styleguide.prototype.compileStyles = require('./gulp/compile.js').styles;

module.exports = Styleguide;
