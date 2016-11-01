const path = require('path');
const fs = require('fs');
const logger = require('./logger');

var defaults = {
    'host': 'localhost',
    'port': '3000',
    'config-name': '_config.json',
    'project-path': process.cwd(),
    srcPath: 'styleguide',
    distPath: 'styleguide/_dist'
}

var Styleguide = function(gulp) {
    // Apply styleguide config file overrides
    this.config = fs.readFileSync(path.join(__dirname, '../styleguide', defaults['config-name']), "utf8");
    if (this.config) {
        this.config = JSON.parse(this.config);
    }

    this.config = Object.assign(defaults, this.config);

    // Apply optional project-level config overrides
    var projectConfigPath = path.join(this.config['project-path'], this.config['config-name'])
    if (fs.existsSync(projectConfigPath)) {
        try {
            this.config = Object.assign(this.config, JSON.parse(fs.readFileSync(projectConfigPath, "utf8")));
            logger.success('Styleguide is configured with: '+ projectConfigPath);
        } catch (err) {
            logger.warn("There was a problem parsing your project\'s "+ this.config['config-name'] +" file... falling back to default config.");
            return null;
        }
    }
    else {
        logger.success('Styleguide is configured with default "_config.json" file');
        logger.warn('(You can override this by creating a "'+ this.config['config-name'] +'" file at the root of your project)');
    }

    process.title = this.config.title || 'styleguide';

    if (this.config.daemon) {
        require('daemon')();
    }

    if (this.config._ && this.config._.length > 0) {
        this.config['project-path'] = this.config._[0];
    }

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
