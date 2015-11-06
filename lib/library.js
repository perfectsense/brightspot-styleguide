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

    if (fs.existsSync(styleguidePath)) {
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
    var targetPath = this._targetPath;
    var webappPath = this._webappPath;

    if (targetPath) {
        rrs(targetPath).forEach(function (filePath) {
            seen[path.relative(targetPath, filePath)] = true;

            callback(filePath);
        });
    }

    if (webappPath) {
        rrs(webappPath).forEach(function (filePath) {
            if (!seen[path.relative(webappPath, filePath)]) {
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

function findWebappFileIn(basePath, file) {
    if (basePath) {
        filePath = path.join(basePath, file);

        if (fs.existsSync(filePath)) {
            return filePath;
        }
    }

    return null;
}

Library.prototype.findWebappFile = function (file) {
    return findWebappFileIn(this._targetPath, file) ||
            findWebappFileIn(this._webappPath, file);
};

module.exports = Library;
