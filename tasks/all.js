var Styleguide = function () {};

Styleguide.prototype.clean = require('./clean.js');
Styleguide.prototype.bower = require('./bower.js').bower;
Styleguide.prototype.compileScripts = require('./compile.js').scripts;
Styleguide.prototype.compileStyles = require('./compile.js').styles;

module.exports = new Styleguide();
