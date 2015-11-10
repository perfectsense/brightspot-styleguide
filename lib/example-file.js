var fs = require('fs');
var handlebars = require('handlebars');
var _ = require('lodash');
var logger = require('./logger');
var path = require('path');
var rrs = require('recursive-readdir-sync');
var traverse = require('traverse');

var DataGenerator = require('./data-generator');

module.exports = function (config, req, res, context) {
    var filePath = context.library.findStyleguideFile(context.requestedPath + '.json');

    if (!filePath) {
        return false;
    }

    // Set up Handlebars cache.
    var compiledTemplates = { };

    function renderTemplate(data) {
        var name = data._template;
        var compiledTemplate = compiledTemplates[name];
        var partialsRegistered;

        if (!compiledTemplate) {
            var templatePath = context.library.findWebappFile(path.join.apply(path, name.split('/')) + '.hbs');

            if (!templatePath) {
                throw new Error(name + " template doesn't exist!");
            }

            var template = fs.readFileSync(templatePath, 'utf8');

            if (/\{\{\s*>/.test(template)) {
                logger.error('Partial deprecated: ' + templatePath);

                if (!partialsRegistered) {
                    context.library.forEachWebappFile(function (filePath) {
                        if (filePath.indexOf('render') > -1) {
                            var name = filePath.split('render')[1].split(/\/(.*)$/)[1].split('.hbs')[0];
                            handlebars.registerPartial(name, fs.readFileSync(filePath, 'utf8'));
                        }
                    });

                    partialsRegistered = true;
                }
            }

            compiledTemplate = handlebars.compile(template);
            compiledTemplates[name] = compiledTemplate;
        }

        return compiledTemplate(data);
    }

    function Template() {
    }

    Template.prototype.toHTML = function () {
        return renderTemplate(this);
    };

    handlebars.registerHelper('render', function (data, fullScope) {
        if (!data) {
            return '';
        }

        if (typeof data !== 'object') {
            return data.toString();
        }

        data = _.extend({ }, data, fullScope.hash);

        return new handlebars.SafeString(renderTemplate(data));
    });

    var data = JSON.parse(fs.readFileSync(filePath, 'utf8'));
    var currentFilePath = filePath;

    function readJson(fileUrl) {
        var filePath = path.join.apply(path, fileUrl.split('/'));

        if (fileUrl.slice(0, 1) === '/') {
            _.each(context.availableLibraries, function (al) {
                var newFilePath = al.library.findStyleguideFile(filePath);

                if (newFilePath) {
                    filePath = newFilePath;

                    return;
                }
            });

            if (!filePath) {
                throw new Error("Can't find " + fileUrl + " in any of the styleguide directories!");
            }

        } else {
            filePath = path.resolve(currentFilePath, '..', filePath);

            if (!filePath) {
                throw new Error("Can't find " + fileUrl + " relative to " + currentFilePath + "!");
            }
        }

        var originalFilePath = currentFilePath;
        currentFilePath = filePath;
        var json = JSON.parse(fs.readFileSync(filePath, 'utf8'));
        currentFilePath = originalFilePath;

        return json;
    }

    traverse(data).forEach(function (value) {
        var dataUrl = value._dataUrl;

        if (dataUrl) {
            this.update(readJson(dataUrl));
        }
    });

    // Validate the JSON data.
    traverse(data).forEach(function (value) {
        if (_.isPlainObject(value)
                && !value._template
                && this.key !== 'options') {

            throw new Error("Object without _template entry at " + this.path.join('/') + "!");
        }
    });

    // Randomize the JSON data.
    new DataGenerator(context.seed).process(data);

    function convert(data) {
        if (typeof data === 'object') {
            if (Array.isArray(data)) {
                return data.map(function (item) {
                    return convert(item);
                });

            } else {
                var copy = data._template ? new Template() : {};

                Object.keys(data).forEach(function (key) {
                    copy[key] = convert(data[key]);
                });

                return copy;
            }
        }

        return data;
    }

    var wrapperUrl = data._wrapper;

    if (wrapperUrl) {
        var wrapper = readJson(wrapperUrl);

        traverse(wrapper).forEach(function (value) {
            if (value._delegate) {
                this.update(data);
            }
        });

        data = wrapper;
    }

    var template = handlebars.compile(fs.readFileSync(context.library.findStyleguideFile('/example-file.hbs'), 'utf8'));

    res.send(template({
        device: req.query.device === 'true',
        data: convert(data)
    }));

    return true;
};
