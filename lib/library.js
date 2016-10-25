var fs = require('fs');
var _ = require('lodash');
var path = require('path');
var rrs = require('recursive-readdir-sync');
var parseString = require('xml2js').parseString;

var logger = require('./logger');
var label = require('./label');

function Library(config, projectRoot) {
    var self = this;

    this._config = config;
    this.name = label(path.basename(path.resolve(projectRoot)));

    if (this._config.v4) {
        var styleguidePath = '.'
    }
    else {
        var styleguidePath = config['styleguide-path']
        // backwards-compat: When the styleguide is a child of the basepath
        if (styleguidePath) {
            styleguidePath = path.join(projectRoot, config['styleguide-path']);
        }
    }

    if (fs.existsSync(styleguidePath) && fs.statSync(styleguidePath).isDirectory()) {
        this._styleguidePath = styleguidePath;
    }

    if (config['project-path'] === projectRoot) {
        var pomPath = path.join(projectRoot, config['maven-pom']);

        if (fs.existsSync(pomPath)) {
            parseString(fs.readFileSync(pomPath), { async: false }, function (err, pomXml) {
                var targetName = pomXml.project.artifactId + '-' + pomXml.project.version;
                var targetPath = path.join(projectRoot, 'target', targetName);

                if (fs.existsSync(targetPath)) {
                    self._targetPath = targetPath;
                }
            });
        }
    }

    var projectRoot = path.join(projectRoot, config['project-src-path']);

    if (fs.existsSync(projectRoot)) {
        this._projectRoot = projectRoot;
    }
}

// Returns the merged Library specific config (if it exists) onto the provided config object
Library.prototype.mergeConfig = function (config) {
    var cfg = this.getConfig();
    if (cfg){
        logger.success('Configured with: '+ path.join(this._styleguidePath, '_config.json'));
        return _.extend({ }, config, cfg);
    }
    logger.success('Configured with default "_config.json" file');
    logger.warn('(You can override this by creating a config file within your project\'s styleguide)');
    return config;
};

// Returns the Library specific config data
Library.prototype.getConfig = function () {
    try {
        return JSON.parse(fs.readFileSync(path.join(this._styleguidePath, '_config.json'), "utf8"));
    } catch (ex) {
        return null;
    }
};

Library.prototype.isLibrary = function () {
    if (this._config.v4) {
        return true
    }
    else {
        return this._styleguidePath || this._projectRoot;
    }
};

Library.prototype.forEachPath = function (callback) {
    if (this._styleguidePath) {
        callback(this._styleguidePath);
    }

    if (this._targetPath) {
        callback(this._targetPath);
    }

    if (this._projectRoot) {
        callback(this._projectRoot);
    }
};

Library.prototype.forEachStyleguideGroup = function (callback) {
    var styleguidePath = this._styleguidePath;

    function excludeFilter(group) {
        if (group.slice(0, 1) === '_'
            || group === 'bower_components'
            || group === 'node_modules') {
            return false
        }
        return true
    }

    if (styleguidePath) {
        fs.readdirSync(styleguidePath).filter(excludeFilter).forEach(function (group) {
            var groupPath = path.join(styleguidePath, group);
            if (fs.statSync(groupPath).isDirectory()) {
                callback(group, groupPath);
            }
        });
    }
};

Library.prototype.forEachProjectFile = function (callback) {
    var seen = { };
    var projectRoot = this._projectRoot;

    if (projectRoot) {
        rrs(projectRoot).forEach(function (filePath) {
            seen[path.relative(projectRoot, filePath)] = true;

            callback(filePath);
        });
    }

    var targetPath = this._targetPath;

    if (targetPath) {
        rrs(targetPath).forEach(function (filePath) {
            if (!seen[path.relative(targetPath, filePath)]) {
                callback(filePath);
            }
        });
    }
};

// prefers the file to be resolved in the styleguide directory, with a fallback on the basepath
Library.prototype.findFile = function (file) {
    return this.findStyleguideFile(file) || this.findAppFile(file);
}

Library.prototype.findStyleguideFile = function (file) {
    var filePath;

    if (this._styleguidePath) {
        filePath = path.join(this._styleguidePath, file);

        if (fs.existsSync(filePath)) {
            return filePath;
        }
    }

    filePath = path.join(__dirname, '..', this._config['styleguide-path'], file);

    if (fs.existsSync(filePath)) {
        return filePath;
    }

    return null;
};

Library.prototype.findAppFile = function (file) {
    function find(projectRoot) {
        if (projectRoot) {
            filePath = path.join(projectRoot, file);

            if (fs.existsSync(filePath)) {
                return filePath;
            }
        }

        return null;
    }

    return find(this._projectRoot) || find(this._targetPath);
};

Library.prototype.findVariableFiles = function () {

    // Find all variable files.
    var varFiles = [ ];
    var projectRoot = this._projectRoot;

    if (projectRoot) {
        rrs(projectRoot).forEach(function (filePath) {
            if (path.extname(filePath) === '.vars') {
                varFiles.push({
                    path: filePath,
                    name: filePath.slice(0, -5).split(fs.sep).join('/')
                });
            }
        });
    }

    // Strip the common prefix from the variable file names.
    var varFilesLength = varFiles.length;

    if (varFilesLength > 1) {
        varFiles.sort(function (x, y) {
            return x.name.localeCompare(y.name);
        });

        var first = varFiles[0];
        var last = varFiles[varFilesLength - 1];
        var commonLength = first.name.length;
        var commonIndex = 0;

        while (commonIndex < commonLength && first.name.charAt(commonIndex) === last.name.charAt(commonIndex)) {
            ++ commonIndex;
        }

        var commonPrefixLength = first.name.substring(0, commonIndex).length;

        varFiles.forEach(function (varFile) {
            varFile.name = varFile.name.substring(commonPrefixLength);
        });

    } else if (varFilesLength > 0) {
        varFiles[0].name = path.basename(varFiles[0].name);
    }

    return varFiles;
};

module.exports = Library;
