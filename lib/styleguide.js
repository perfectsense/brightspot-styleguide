const path = require('path');
const fs = require('fs');

var defaults = {
    srcPath: 'styleguide',
    distPath: 'styleguide/_dist'
}

var Styleguide = function(gulp, config) {
    var config = Object.assign({ }, defaults, config);

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

    // Register Styleguide tasks with gulp
    require('./util').loadModules("./gulp/tasks", gulp)

    this.task = {
        lint: {
            less: () => 'lint:less',
            js: () => 'lint:js',
            json: () => 'lint:json'
        }
    }

    this.path = {
        src: (_path) => {
            return path.join(this.config.srcPath, _path || '')
        },

        dist: (_path) => {
            return path.join(this.config.distPath, _path || '')
        }
    }
}


Styleguide.prototype.serve = function() {
    return require('./server')(this.config)
};

module.exports = Styleguide;
