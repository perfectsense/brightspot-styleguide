var fs = require('fs');
var _ = require('lodash');
var path = require('path');
var rrs = require('recursive-readdir-sync');
var parseString = require('xml2js').parseString;

var logger = require('./logger');
var label = require('./label');

function Library(config, projectPath) {
    var self = this;

    this._config = config;
    this.name = label(path.basename(path.resolve(projectPath)));

    if (this._config.v4) {
        var projectSrcPath = config['project-src-path']
        if (projectSrcPath) {
            projectSrcPath = path.join(projectPath, config['project-src-path']);
        }
    }
    else {
        var projectSrcPath = config['styleguide-path']
        if (projectSrcPath) {
            projectSrcPath = path.join(projectPath, config['styleguide-path']);
        }
    }

    if (fs.existsSync(projectSrcPath) && fs.statSync(projectSrcPath).isDirectory()) {
        this._projectSrcPath = projectSrcPath;
    }

    if (config['project-path'] === projectPath) {
        var pomPath = path.join(projectPath, config['maven-pom']);

        if (fs.existsSync(pomPath)) {
            parseString(fs.readFileSync(pomPath), { async: false }, function (err, pomXml) {
                var targetName = pomXml.project.artifactId + '-' + pomXml.project.version;
                var targetPath = path.join(projectPath, 'target', targetName);

                if (fs.existsSync(targetPath)) {
                    self._targetPath = targetPath;
                }
            });
        }
    }

    var projectPath = path.join(projectPath, config['project-src-path']);

    if (fs.existsSync(projectPath)) {
        this._projectPath = projectPath;
    }
}

// Returns the merged Library specific config (if it exists) onto the provided config object
Library.prototype.mergeConfig = function (config) {
    var cfg = this.getConfig();
    if (cfg){
        logger.success('Configured with: '+ path.join(this._projectSrcPath, '_config.json'));
        return _.extend({ }, config, cfg);
    }
    logger.success('Configured with default "_config.json" file');
    logger.warn('(You can override this by creating a config file within your project\'s styleguide)');
    return config;
};

// Returns the Library specific config data
Library.prototype.getConfig = function () {
    try {
        return JSON.parse(fs.readFileSync(path.join(this._projectSrcPath, '_config.json'), "utf8"));
    } catch (ex) {
        return null;
    }
};

Library.prototype.isLibrary = function () {
    if (this._config.v4) {
        return true
    }
    else {
        return this._projectSrcPath || this._projectPath;
    }
};

Library.prototype.forEachPath = function (callback) {
    if (this._projectSrcPath) {
        callback(this._projectSrcPath);
    }

    if (this._targetPath) {
        callback(this._targetPath);
    }

    if (this._projectPath) {
        callback(this._projectPath);
    }
};

Library.prototype.forEachStyleguideGroup = function (callback) {
    var projectSrcPath = this._projectSrcPath;

    function excludeFilter(group) {
        if (group.slice(0, 1) === '_'
            || group === 'bower_components'
            || group === 'node_modules') {
            return false
        }
        return true
    }

    if (projectSrcPath) {
        fs.readdirSync(projectSrcPath).filter(excludeFilter).forEach(function (group) {
            var groupPath = path.join(projectSrcPath, group);
            if (fs.statSync(groupPath).isDirectory()) {
                callback(group, groupPath);
            }
        });
    }
};

Library.prototype.forEachProjectFile = function (callback) {
debugger
    var seen = { };
    var projectPath = this._projectPath;

    if (projectPath) {
        rrs(projectPath).forEach(function (filePath) {
            seen[path.relative(projectPath, filePath)] = true;

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

    if (this._projectSrcPath) {
        filePath = path.join(this._projectSrcPath, file);

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
    function find(projectPath) {
        if (projectPath) {
            filePath = path.join(projectPath, file);

            if (fs.existsSync(filePath)) {
                return filePath;
            }
        }

        return null;
    }

    return find(this._projectPath) || find(this._targetPath);
};

Library.prototype.findVariableFiles = function () {

    // Find all variable files.
    var varFiles = [ ];
    var projectPath = this._projectPath;

    if (projectPath) {
        rrs(projectPath).forEach(function (filePath) {
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
