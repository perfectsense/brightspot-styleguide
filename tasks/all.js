var Styleguide = function () {};

Styleguide.prototype.clean = require('./clean.js').clean;
Styleguide.prototype.copySrc = require('./clean.js').copySrc;
Styleguide.prototype.bowerInstall = require('./bower.js').bowerInstall;
Styleguide.prototype.copyBower = require('./bower.js').copyBower;
Styleguide.prototype.compileScripts = require('./compile.js').scripts;
Styleguide.prototype.compileStyles = require('./compile.js').styles;

module.exports = new Styleguide();
