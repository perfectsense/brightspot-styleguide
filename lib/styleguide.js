var path = require('path');
var fs = require('fs');

var Styleguide = function(config) {
    var config = config || {};

    process.title = config.title || 'styleguide';

    if (config.daemon) {
        require('daemon')();
    }

    if (config._ && config._.length > 0) {
        config['project-path'] = config._[0];
    }

    var configFile = fs.readFileSync(path.join(__dirname, '../styleguide', '_config.json'), "utf8");

    // Merge the command line args onto the _config file
    this.config = Object.assign({ }, JSON.parse(configFile), config);
}

Styleguide.prototype.srcPath = function() {
    return 'src'
};

Styleguide.prototype.distPath = function() {
    return '_dist'
};

Styleguide.prototype.serve = function() {
    return require('server')(this.config)
};

module.exports = Styleguide;
