var fs = require('fs');
var handlebars = require('handlebars');
var _ = require('lodash');
var $ = require('cheerio');
var logger = require('./logger');
var path = require('path');
var rrs = require('recursive-readdir-sync');
var traverse = require('traverse');
var glob = require("glob");

module.exports = function (config, context) {
    var selectedLibrary = context.library;
    var projectLibrary = config.projectLibrary;
    var filePath = selectedLibrary.findStyleguideFile(path.join(context.requestedPath, '_documentation.json'));

    if (!filePath) {
        return false;
    }

    var documentationHtmlPaths = glob.sync("jsdocs/**/*.html", {
        cwd: projectLibrary._targetPath
    });

    var data = JSON.parse(fs.readFileSync(filePath, 'utf8'));
    var jsDocMarkup;

    // each javascript include path defined in the data should be encoded
    // to match the format of the corresponding jsdoc file path
    _.each(data.jsDocIncludes, function(include) {
        var normalizedPath = path.basename(include.replace(/\//g, '_'), '.js');

        var documentationFilePath = _.find(documentationHtmlPaths, function(o){
            if (o.indexOf(normalizedPath) > 0){
                return o;
            }
        });

        if (documentationFilePath) {
            var jsDocPath = path.join(projectLibrary._targetPath, documentationFilePath);
            // TODO: need to append here so that multiple jsdocs will render
            jsDocMarkup = $(fs.readFileSync(jsDocPath, 'utf8'));
        }
    });

    data.jsDoc = jsDocMarkup.find('#main section').html();
    context.documentation = data;

    return;
}
