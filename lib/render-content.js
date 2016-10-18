var fs = require('fs');
var handlebars = require('handlebars');
var _ = require('lodash');
var path = require('path');
var traverse = require('traverse');

var logger = require('./logger');
var label = require('./label');
var Library = require('./library');


function read(context, filePath) {
    return fs.readFileSync(context.library.findStyleguideFile(filePath), 'utf8');
}

module.exports = function (config, req, res, context, exampleFilePath, templateFile) {
    // Finds all example files mapped to the request URL.
    var match = context.requestedUrl.match(/\.(json)$/i);

    if (!match) {
        return false;
    }

    var files = context.files = [ ];
    var mainFile;
    var examplePath = path.dirname(exampleFilePath);
    var file = path.basename(exampleFilePath);


        if (file.slice(0, 1) !== '_' && path.extname(file) === config['json-suffix']) {
            var options;
            var exampleFileData = fs.readFileSync(exampleFilePath, 'utf8');

            traverse(JSON.parse(exampleFileData)).forEach(function (value) {

                if (this.key === 'displayOptions' &&
                        _.isPlainObject(value)) {

                    var prefix = this.path.join('/') + '/';

                    Object.keys(value).forEach(function (key) {
                        if (!options) {
                            options = { };
                        }

                        options[prefix + key] = value[key];
                    });
                }
            });

            var name = path.basename(examplePath, config['json-suffix']);
            var url = req.path.slice(0, -config['json-suffix'].length);
            var f = {
                name: label(name),
                url: url,
                jsonUrl: url + '.json',
                exampleFilePath: exampleFilePath,
                exampleFileData: exampleFileData,
                options: options
            };

            var descriptionPath = exampleFilePath.slice(0, -config['json-suffix'].length) + '.md';

            if (fs.existsSync(descriptionPath)) {
                f.description = marked(fs.readFileSync(descriptionPath, 'utf8'));
            }

            if (file === 'main.json') {
                mainFile = f;

            } else {
                files.push(f);
            }

            if (fs.existsSync(examplePath + '/_config.json')) {
                config.example = JSON.parse(fs.readFileSync(examplePath + '/_config.json', 'utf-8'));
            }
            else {
                config.example = null;
            }
        }

    // Display the main example file first.
    if (mainFile) {
        files.unshift(mainFile);
    }

    handlebars.registerPartial('layout', read(context, '/content.hbs'));

    if (config.example){
        context.example = config.example;
    }

    var template = handlebars.compile(read(context, templateFile), {
        preventIndent: true
    });
    return res.send(template(context))


};
