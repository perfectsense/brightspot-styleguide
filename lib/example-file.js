var fs = require('fs');
var handlebars = require('handlebars');
var _ = require('lodash');
var logger = require('./logger');
var path = require('path');
var rrs = require('recursive-readdir-sync');
var traverse = require('traverse');

var DataGenerator = require('./data-generator');

function resolveTemplatePath(data, library) {
    traverse(data).forEach(function (value) {
        var template = value._template;

        if (template) {
            value._template = library.findWebappFile(path.join.apply(path, template.split('/')) + '.hbs');

            if (!value._template) {
                throw new Error(template + " template doesn't exist!");
            }
        }
    });
}

module.exports = function (config, req, res, context) {
    var selectedLibrary = context.library;
    var filePath = selectedLibrary.findStyleguideFile(context.requestedPath + '.json');

    if (!filePath) {
        return false;
    }

    var data = JSON.parse(fs.readFileSync(filePath, 'utf8'));

    // Resolve all _dataUrl references.
    var currentDataPath = filePath;

    traverse(data).forEach(function (value) {
        var dataUrl = value._dataUrl;

        if (dataUrl) {
            var dataPath = path.join.apply(path, dataUrl.split('/'));

            if (dataUrl.slice(0, 1) === '/') {
                dataPath = selectedLibrary.findStyleguideFile(dataPath);

                if (!dataPath) {
                    throw new Error("Can't find " + dataUrl + " in " + selectedLibrary.name + " styleguide!");
                }

            } else {
                dataPath = path.resolve(currentDataPath, '..', dataPath);

                if (!dataPath) {
                    throw new Error("Can't find " + dataUrl + " relative to " + currentDataPath + "!");
                }
            }

            var originalDataPath = currentDataPath;
            currentDataPath = dataPath;

            this.update(JSON.parse(fs.readFileSync(dataPath, 'utf8')));

            currentDataPath = originalDataPath;
        }
    });

    // Resolve all _template paths.
    resolveTemplatePath(data, selectedLibrary);

    // Validate the JSON data.
    traverse(data).forEach(function (value) {
        if (_.isPlainObject(value)
                && !value._template
                && this.key.slice(0, 1) !== '_'
                && this.key !== 'displayOptions'
                && this.key !== 'extraAttributes') {

            throw new Error("Object without _template entry at " + this.path.join('/') + "!");
        }
    });

    // Randomize the JSON data.
    new DataGenerator(context.seed).process(data);

    // Wrap the example file data?
    var projectLibrary = config.projectLibrary;

    if (data._wrapper !== false) {
        var wrapperPath = projectLibrary.findStyleguideFile(data._wrapper ? data._wrapper : '_wrapper.json');

        while (wrapperPath) {
            var wrapper = JSON.parse(fs.readFileSync(wrapperPath, 'utf8'));

            resolveTemplatePath(wrapper, projectLibrary);

            traverse(wrapper).forEach(function (value) {
                if (value._delegate) {
                    this.update(data);
                }
            });

            data = wrapper;
            wrapperPath = data._wrapper ? projectLibrary.findStyleguideFile(data._wrapper) : null;
        }
    }

    // Set up Handlebars cache.
    var compiledTemplates = { };
    var partialsRegistered;

    function renderTemplate(data) {
        var templatePath = data._template;
        var compiledTemplate = compiledTemplates[templatePath];

        if (!compiledTemplate) {
            var template = fs.readFileSync(templatePath, 'utf8');

            // Hack to support legacy templates.
            if (/\{\{\s*>/.test(template)) {
                logger.error('Partial deprecated: ' + templatePath);

                if (!partialsRegistered) {
                    projectLibrary.forEachWebappFile(function (filePath) {
                        if (filePath.indexOf('render') > -1) {
                            handlebars.registerPartial(
                                    'render/' + filePath.split('render')[1].split(/\/(.*)$/)[1].split('.hbs')[0],
                                    fs.readFileSync(filePath, 'utf8'));
                        }
                    });

                    partialsRegistered = true;
                }
            }

            compiledTemplate = handlebars.compile(template);
            compiledTemplates[templatePath] = compiledTemplate;
        }

        return compiledTemplate(data);
    }

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

    // Render the example file data.
    var template = handlebars.compile(fs.readFileSync(projectLibrary.findStyleguideFile('/example-file.hbs'), 'utf8'));

    function Template() {
    }

    Template.prototype.toHTML = function () {
        return renderTemplate(this);
    };

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

    // This helper returns the key/value pairs of extraAttributes key as a string. Used in links, images, forms.
    handlebars.registerHelper('extraAttributes', function (context, options) {
        var extraAttributes = '';
        var attributesObject;

        if (!context) {
            return;
        }

        if (!context.data.root.extraAttributes) {
            return;
        }

        attributesObject = context.data.root.extraAttributes;

        for (var key in attributesObject) {
            extraAttributes += key + '=' + attributesObject[key];
        }

        return new handlebars.SafeString(extraAttributes);
    });

    res.send(template({
        device: req.query.device === 'true',
        data: convert(data)
    }));

    return true;
};
