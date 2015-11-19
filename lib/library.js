var fs = require('fs');
var _ = require('lodash');
var path = require('path');
var rrs = require('recursive-readdir-sync');
var xml2json = require('xml2json');

var label = require('./label');

function Library(config, basePath) {

    this._config = config;
    this.name = label(path.basename(path.resolve(basePath)));

    var styleguidePath = path.join(basePath, config['styleguide-path']);

    if (fs.existsSync(styleguidePath) && fs.statSync(styleguidePath).isDirectory()) {
        this._styleguidePath = styleguidePath;
    }

    if (config['project-path'] === basePath) {
        var pomPath = path.join(basePath, config['maven-pom']);

        if (fs.existsSync(pomPath)) {
            var pomXml = xml2json.toJson(fs.readFileSync(pomPath), { object: true });
            var targetName = pomXml.project.artifactId + '-' + pomXml.project.version;
            var targetPath = path.join(basePath, 'target', targetName);

            if (fs.existsSync(targetPath)) {
                this._targetPath = targetPath;
            }
        }
    }

    var webappPath = path.join(basePath, config['maven-webapp-path']);

    if (fs.existsSync(webappPath)) {
        this._webappPath = webappPath;
    }
}

// Returns the Library specific `_config.json` data
Library.prototype.getConfig = function () {
    try {
        return JSON.parse(fs.readFileSync(path.join(this._styleguidePath, '_config.json'), "utf8"));
    } catch (ex) {
        return null;
    }
};

Library.prototype.isLibrary = function () {
    return this._styleguidePath || this._webappPath;
};

Library.prototype.forEachPath = function (callback) {
    if (this._styleguidePath) {
        callback(this._styleguidePath);
    }

    if (this._targetPath) {
        callback(this._targetPath);
    }

    if (this._webappPath) {
        callback(this._webappPath);
    }
};

Library.prototype.forEachStyleguideGroup = function (callback) {
    var styleguidePath = this._styleguidePath;

    if (styleguidePath) {
        fs.readdirSync(styleguidePath).forEach(function (group) {
            if (group.slice(0, 1) !== '_') {
                var groupPath = path.join(styleguidePath, group);

                if (fs.statSync(groupPath).isDirectory()) {
                    callback(group, groupPath);
                }
            }
        });
    }
};

Library.prototype.forEachWebappFile = function (callback) {
    var seen = { };
    var webappPath = this._webappPath;

    if (webappPath) {
        rrs(webappPath).forEach(function (filePath) {
            seen[path.relative(webappPath, filePath)] = true;

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

Library.prototype.findWebappFile = function (file) {
    function find(basePath) {
        if (basePath) {
            filePath = path.join(basePath, file);

            if (fs.existsSync(filePath)) {
                return filePath;
            }
        }

        return null;
    }

    return find(this._webappPath) || find(this._targetPath);
};

Library.prototype.findVariableFiles = function () {

    // Find all variable files.
    var varFiles = [ ];
    var webappPath = this._webappPath;

    if (webappPath) {
        rrs(webappPath).forEach(function (filePath) {
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
