var Styleguide = function (config) {
    this.config = config
}

Styleguide.prototype.clean = require('./tasks/clean.js').clean;
Styleguide.prototype.copySrc = require('./tasks/clean.js').copySrc;
Styleguide.prototype.bowerInstall = require('./tasks/bower.js').bowerInstall;
Styleguide.prototype.copyBower = require('./tasks/bower.js').copyBower;
Styleguide.prototype.compileScripts = require('./tasks/compile.js').scripts;
Styleguide.prototype.compileStyles = require('./tasks/compile.js').styles;

module.exports = Styleguide;
