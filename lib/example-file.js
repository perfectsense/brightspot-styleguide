var fs = require('fs');
var handlebars = require('handlebars');
var _ = require('lodash');
var logger = require('./logger');
var path = require('path');
var rrs = require('recursive-readdir-sync');
var traverse = require('traverse');

var DataGenerator = require('./data-generator');

module.exports = function (config, req, res, context) {
    var filePath = config.styleguidePaths.find(context.requestedPath + '.json');

    if (!filePath) {
        return false;
    }

    // Set up Handlebars cache.
    var compiledTemplates = { };

    function renderTemplate(context) {
        var name = context._template;
        var compiledTemplate = compiledTemplates[name];
        var partialsRegistered;

        if (!compiledTemplate) {
            var namePath = path.join.apply(path, name.split('/'));

            var prependedPaths = config.webappPaths.map(function (webappPath) {
                return path.join(webappPath, 'render', namePath + '.hbs');
            });

            var templatePath = _.find(prependedPaths, function (prependedPath) {
                return fs.existsSync(prependedPath);
            });

            if (!templatePath) {
                throw new Error(name + " template doesn't exist!");
            }

            var template = fs.readFileSync(templatePath, 'utf8');

            if (/\{\{\s*>/.test(template)) {
                logger.error('Partial deprecated: ' + templatePath);

                if (!partialsRegistered) {
                    config.webappPaths.forEach(function (webappPath) {
                        rrs(webappPath).forEach(function (filePath) {
                            if (filePath.indexOf('render') > -1) {
                                var name = filePath.split('render')[1].split(/\/(.*)$/)[1].split('.hbs')[0];
                                handlebars.registerPartial(name, fs.readFileSync(filePath, 'utf8'));
                            }
                        });
                    });

                    partialsRegistered = true;
                }
            }

            compiledTemplate = handlebars.compile(template);
            compiledTemplates[name] = compiledTemplate;
        }

        return compiledTemplate(context);
    }

    function Template() {
    }

    Template.prototype.toHTML = function () {
        return renderTemplate(this);
    };

    handlebars.registerHelper('render', function (context, fullScope) {
        if (!context) {
            return '';
        }

        if (typeof context !== 'object') {
            return context.toString();
        }

        context = _.extend({ }, context, fullScope.hash);

        return new handlebars.SafeString(renderTemplate(context));
    });

    var data = JSON.parse(fs.readFileSync(filePath, 'utf8'));
    var currentDataPath = filePath;

    traverse(data).forEach(function (value) {
        var dataUrl = value._dataUrl;

        if (dataUrl) {
            var dataPath = path.join.apply(path, dataUrl.split('/'));

            if (dataUrl.slice(0, 1) === '/') {
                dataPath = config.styleguidePaths.find(dataPath);

                if (!dataPath) {
                    throw new Error("Can't find " + dataUrl + " in any of the styleguide directories!");
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

    var template = handlebars.compile(fs.readFileSync(config.styleguidePaths.find(req.query.ajax ? '/ajax.hbs' : '/iframe.hbs'), 'utf8'));

    // if we have a body template, we do need the iframe.hbs to drop us a body tag
    // as it means we are creating a full styleguide page, which has it's own body renderer
    if(data._template === 'common/body') {
        data.body = false;
    } else {
        data.body = true;
    }

    res.send(template({
        device: req.query.device === 'true',
        data: convert(data)
    }));

    return true;
};
