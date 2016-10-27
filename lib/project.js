var fs = require('fs');
var _ = require('lodash');
var path = require('path');
var rrs = require('recursive-readdir-sync');
var parseString = require('xml2js').parseString;

var logger = require('./logger');
var label = require('./label');

function Project(config, projectPath) {
    var self = this;

    this._config = config;
    this.name = label(path.basename(path.resolve(projectPath)));

    if (fs.existsSync(projectPath)) {
        this._projectPath = projectPath;
    }

    // Project has a 'styleguide' directory?
    if (fs.existsSync(path.join(this._projectPath, 'styleguide'))) {
        this._config['legacy-project'] = true;
    }

    // Legacy projects define a separate path for their styleguide
    if (this._config['legacy-project']) {
        this._styleguidePath = path.join(config['project-path'], config['styleguide-path'])
    }

    var projectSrcPath = config['project-src-path']
    if (projectSrcPath) {
        projectSrcPath = path.join(this._projectPath, config['project-src-path']);
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
}

// Returns the merged Project specific config (if it exists) onto the provided config object
Project.prototype.mergeConfig = function (config) {
    var cfg = this.getConfig();
    if (cfg){
        logger.success('Configured with: '+ path.join(this._projectSrcPath, '_config.json'));
        return _.extend({ }, config, cfg);
    }
    logger.success('Configured with default "_config.json" file');
    logger.warn('(You can override this by creating an "_config.json" file within your project\'s styleguide)');
    return config;
};


Project.prototype.getConfigPath = function () {
    var configName = "_config.json"
    var filePath

    // prefer the config in the source path
    filePath = path.join(this._projectSrcPath, configName)
    if (fs.existsSync(filePath)) {
        return filePath
    }

    // fallback on the styleguide path
    filePath = path.join(this._styleguidePath, configName)
    if (fs.existsSync(filePath)) {
        return filePath
    }
};

// Returns the Project specific config data
Project.prototype.getConfig = function () {
    var filePath = this.getConfigPath()
    try {
        return JSON.parse(fs.readFileSync(filePath, "utf8"));
    } catch (ex) {
        logger.warn("Project config file was unable to be parsed");
        return null;
    }
};

Project.prototype.isProject = function () {
    return this._projectSrcPath || this._projectPath;
};

Project.prototype.forEachPath = function (callback) {
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

Project.prototype.forEachStyleguideGroup = function (callback) {
    var projectSrcPath = this._styleguidePath || this._projectSrcPath;

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

Project.prototype.forEachProjectFile = function (callback) {
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

Project.prototype.findFile = function (file, relPath) {
    // can we resolve the file using the relative path?
    if (relPath) {
        var filePath = path.resolve(relPath, file);
        if (fs.existsSync(filePath)) {
            return filePath;
        }
    }

    // go look elsewhere
    return this.findStyleguideFile(file) || this.findProjectFile(file);
}

// for backwards compatability where the styleguide files live outside the sourcePath
Project.prototype.findStyleguideFile = function (file) {
    var filePath;
    var styleguidePath = this._styleguidePath || this._projectSrcPath;

    if (styleguidePath) {
        filePath = path.join(styleguidePath, file);

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

Project.prototype.findProjectFile = function (file) {
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

Project.prototype.findVariableFiles = function () {

    // Find all variable files.
    var varFiles = [ ];
    var projectSrcPath = this._projectSrcPath;

    if (projectSrcPath) {
        rrs(projectSrcPath).forEach(function (filePath) {
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

module.exports = Project;
