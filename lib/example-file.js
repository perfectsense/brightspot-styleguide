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

function resolveDataUrlPaths(data, currentDataPath, library) {

    traverse(data).forEach(function (value) {
        var dataUrl = value._dataUrl;

        if (dataUrl) {
            var dataPath = path.join.apply(path, dataUrl.split('/'));

            if (dataUrl.slice(0, 1) === '/') {
                dataPath = library.findStyleguideFile(dataPath);

                if (!dataPath) {
                    throw new Error("Can't find " + dataUrl + " in " + library.name + " styleguide!");
                }

            } else {
                dataPath = path.resolve(currentDataPath, '..', dataPath);

                if (!dataPath) {
                    throw new Error("Can't find " + dataUrl + " relative to " + currentDataPath + "!");
                }
            }

            var originalDataPath = currentDataPath;
            currentDataPath = dataPath;

            // extend the incoming data from `dataUrl` with this node's data, then update the node
            this.update(_.extend({ }, JSON.parse(fs.readFileSync(dataPath, 'utf8')), this.node));

            currentDataPath = originalDataPath;
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

    // Resolve all _dataUrl paths
    resolveDataUrlPaths(data, filePath, selectedLibrary);

    // Resolve all _template paths.
    resolveTemplatePath(data, selectedLibrary);

    // Validate the JSON data. Exceptions for the special keys we have that are maps, so they don't need _template
    traverse(data).forEach(function (value) {
        if (_.isPlainObject(value)
                && !value._template
                && this.key.slice(0, 1) !== '_'
                && this.key !== 'displayOptions'
                && this.key !== 'extraAttributes'
                && this.key !== 'jsonObject') {

            var safeParent = false;

            this.parents.forEach(function (value) {
                if(this.key !== 'jsonObject') {
                    safeParent = true;
                }
            });

            if(!safeParent) {
                throw new Error("Object without _template entry at " + this.path.join('/') + "!");
            }
        }
    });

    var projectLibrary = config.projectLibrary;

    // Wrap the example file data?
    if (data._wrapper !== false) {
        var wrapperPath = projectLibrary.findStyleguideFile(data._wrapper ? data._wrapper : '_wrapper.json');

        while (wrapperPath) {
            var wrapper = JSON.parse(fs.readFileSync(wrapperPath, 'utf8'));

            resolveDataUrlPaths(wrapper, wrapperPath, projectLibrary);

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

    // post-process the JSON data.
    new DataGenerator(context).process(data);

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

    // This helper returns the key/value pairs of extraAttributes key , separated with '=' as a string. Used in links, images, forms.
    handlebars.registerHelper('extraAttributes', function (context, options) {
        var extraAttributes = '';
        var extraAttributesData;

        if (!context) {
            return;
        }

        if (!context.data.root.extraAttributes) {
            return;
        }

        extraAttributesData = context.data.root.extraAttributes;

        for (var key in extraAttributesData) {
            extraAttributes += ' ' + key + '="' + extraAttributesData[key] + '"';
        }

        return new handlebars.SafeString(extraAttributes);
    });

    // This helper returns the key/value pairs of jsonObject key as an object string. Used when we need to pass a JSON object as a string into some JS options
    handlebars.registerHelper('jsonObject', function (context, options) {
        var jsonObjectData;
        var count = 0;

        if (!context) {
            return;
        }

        if (!context.data.root.jsonObject) {
            return;
        }

        jsonObjectData = context.data.root.jsonObject;

        return new handlebars.SafeString(JSON.stringify(jsonObjectData));
    });

    // Defines an element by associating it with a template.
    var ELEMENT_DATA_PREFIX = '_brightspot_element_';

    handlebars.registerHelper('defineElement', function (name, options) {
        this[ELEMENT_DATA_PREFIX + name] = options;
    });

    // Renders the element template.
    handlebars.registerHelper('element', function (name) {
        var elementOptions = this[ELEMENT_DATA_PREFIX + name];

        if (!elementOptions) {
            throw new Error('[' + name + '] element not defined!');
        }

        var value = this[name];

        if (elementOptions.hash.noWith) {
            value = elementOptions.fn(this);

        } else if (value) {
            value = elementOptions.fn(value);

        } else {
            value = elementOptions.inverse(value);
        }

        return new handlebars.SafeString(value);
    });

    // Marks the template within as being overrideable.
    var OVERRIDE_TEMPLATE_DATA = '_brightspot_overrideTemplate';

    handlebars.registerHelper('overridable', function (options) {
        var overrideTemplate = this[OVERRIDE_TEMPLATE_DATA];
        var override;

        if (overrideTemplate) {
            override = overrideTemplate(this);

        } else {
            override = options.fn(this);
        }

        return new handlebars.SafeString(override);
    });

    // Renders the template replacing the overrideable area.
    handlebars.registerHelper('override', function (name, options) {
        var template = handlebars.compile(fs.readFileSync(selectedLibrary.findWebappFile(name + '.hbs'), 'utf8'));
        this[OVERRIDE_TEMPLATE_DATA] = options.fn;

        return new handlebars.SafeString(template(this));
    });

    var iframeJs = fs.readFileSync(path.join(__dirname, '..', 'node_modules/iframe-resizer/js/iframeResizer.contentWindow.min.js'), 'utf8');

    // if the example document contains a </head> then we append the iframe javascript to the <head>
    res.send(template({
        device: req.query.device === 'true',
        data: convert(data)
    }).replace(/\<\/head\>/, "<script>"+ iframeJs +"</script>\n</head>"));

    return true;
};
