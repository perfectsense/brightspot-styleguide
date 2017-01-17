var fs = require('fs');
var handlebars = require('handlebars');
var _ = require('lodash');
var escapeHtml = require('escape-html-in-json');
var logger = require('./logger');
var path = require('path');
var rrs = require('recursive-readdir-sync');
var traverse = require('traverse');

var DataGenerator = require('./data-generator');

function resolveTemplatePath(data, library) {
    traverse(data).forEach(function (value) {
        var template = value._template;

        if (template) {
            value._template = path.join.apply(path, template.split('/'))
            value._template += (path.extname(value._template) === '') ? '.hbs' : ''
            value._template = library.findWebappFile( value._template );

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

    // Validate the JSON data. Exceptions for the special keys we have that are maps, so they don't need _template or _view
    traverse(data).forEach(function (value) {
        if (_.isPlainObject(value)
            && !value._template
            && !value._view
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
                throw new Error("Object without _template or _view entry at " + this.path.join('/') + "!");
            }
        }
    });

    var projectLibrary = config.projectLibrary;

    // Wrap the example file data?
    if (data._wrapper !== false) {
        var effectiveWrapperLibrary = null;
        var defaultWrapperName = null;

        if (data._view) {
            effectiveWrapperLibrary = selectedLibrary;
            defaultWrapperName = '_viewWrapper.json';

        } else {
            effectiveWrapperLibrary = projectLibrary;
            defaultWrapperName = '_wrapper.json';
        }

        var wrapperPath = effectiveWrapperLibrary.findStyleguideFile(data._wrapper ? data._wrapper : defaultWrapperName);

        while (wrapperPath) {
            var wrapper = JSON.parse(fs.readFileSync(wrapperPath, 'utf8'));

            resolveDataUrlPaths(wrapper, wrapperPath, effectiveWrapperLibrary);

            if (!data._view) {
                resolveTemplatePath(wrapper, effectiveWrapperLibrary);
            }

            traverse(wrapper).forEach(function (value) {
                if (value._delegate) {
                    this.update(data);
                }
            });

            data = wrapper;

            wrapperPath = data._wrapper ? effectiveWrapperLibrary.findStyleguideFile(data._wrapper) : null;
        }
    }

    // post-process the JSON data.
    new DataGenerator(context, config).process(data);

    // Set up Handlebars cache.
    var compiledTemplates = { };
    var partialsRegistered;

    function renderTemplate(data) {
        var templatePath = data._template;

        // See if this is a JSON view and render the data as JSON using a simple HTML template
        if (!templatePath && data._view) {
            return renderJsonTemplate(data);
        }

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

    function renderJsonTemplate(data) {
        var jsonData = {
            "json": JSON.stringify(removePrivateKeys(data), escapeHtml, 2)
        };
        var jsonTemplate = fs.readFileSync(context.library.findStyleguideFile('/example-json.hbs'), 'utf8');
        var jsonCompiledTemplate = handlebars.compile(jsonTemplate);

        return jsonCompiledTemplate(jsonData);
    }

    // Returns all pairs who's keyname doesn't start with an '_'
    function removePrivateKeys(data) {
        return _.omit(data, function(value, key){
            return _.startsWith(key, '_');
        });
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
                var copy = {};
                if (data._template || data._view) {
                    copy = new Template()
                }

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

    // Defines a block.
    var DATA_PREFIX = '_brightspot_';
    var ELEMENT_DATA = DATA_PREFIX + 'element';
    var BLOCK_NAME_DATA = DATA_PREFIX + 'blockName';
    var DEFINE_BLOCK_CONTAINER_IN_EXTEND_DATA = DATA_PREFIX + 'defineBlockContainerInExtend';

    function compile(name) {
        return handlebars.compile(fs.readFileSync(selectedLibrary.findWebappFile(name + '.hbs'), 'utf8'));
    }

    handlebars.registerHelper('defineBlock', function (name, options) {
        if (!options.data[BLOCK_NAME_DATA]) {
            options.data[BLOCK_NAME_DATA] = name;
        }

        var extend = options.hash.extend;

        if (extend) {
            var template = compile(extend);
            var templateOptions = { data: { } };
            templateOptions.data[BLOCK_NAME_DATA] = name;
            templateOptions.data[DEFINE_BLOCK_CONTAINER_IN_EXTEND_DATA] = true;

            template(this, templateOptions);
        }

        return new handlebars.SafeString(options.fn(this));
    });

    // Marks the template as the block container.
    handlebars.registerHelper('defineBlockContainer', function (options) {
        if (options.data[DEFINE_BLOCK_CONTAINER_IN_EXTEND_DATA]) {
            return null;

        } else {
            return new handlebars.SafeString(options.fn(this));
        }
    });

    // Marks the template as the block body.
    var BLOCK_BODY_TEMPLATE_DATA = DATA_PREFIX + 'blockBodyTemplate';

    function defineBlockBody(options) {
        var overrideTemplate = this[BLOCK_BODY_TEMPLATE_DATA];
        var override;

        if (overrideTemplate) {
            override = overrideTemplate(this, options);

        } else {
            override = options.fn(this);
        }

        return new handlebars.SafeString(override);
    }

    handlebars.registerHelper('defineBlockBody', defineBlockBody);
    handlebars.registerHelper('defaultBlockBody', defineBlockBody);

    // Returns the name of the current block.
    handlebars.registerHelper('blockName', function (options) {
        return options.data[BLOCK_NAME_DATA];
    });

    // Renders the named block, optionally replacing its body.
    handlebars.registerHelper('block', function (name, options) {
        var template = compile(name);
        this[BLOCK_BODY_TEMPLATE_DATA] = options.fn;
        var templateOptions = { data: { } };
        templateOptions.data[BLOCK_NAME_DATA] = options.hash.name;

        return new handlebars.SafeString(template(this, templateOptions));
    });

    // Defines an element.
    var ELEMENT_NAME_DATA = DATA_PREFIX + 'elementName';
    var ELEMENT_DEFINITION_DATA_PREFIX = DATA_PREFIX + 'element_';

    handlebars.registerHelper('defineElement', function (name, options) {
        this[ELEMENT_DEFINITION_DATA_PREFIX + name] = options;
    });

    // Returns the name of the current element.
    handlebars.registerHelper('elementName', function (options) {
        return options.data[ELEMENT_NAME_DATA];
    });

    // Renders the named element.
    handlebars.registerHelper('element', function (name, options) {
        var self = this;
        var elementOptions = this[ELEMENT_DEFINITION_DATA_PREFIX + name];

        if (!elementOptions) {
            throw new Error('[' + name + '] element not defined!');
        }

        // does this element contain nested elements?
        Object.keys(this).filter(function (key) {
            if (key.indexOf(ELEMENT_DATA) > -1 && options.data[BLOCK_NAME_DATA]) {
                self[key].data[BLOCK_NAME_DATA] = options.data[BLOCK_NAME_DATA];
            }
        });

        var value = this[name];
        var fnOptions = { data: { } };
        var blockName = options.data[BLOCK_NAME_DATA] || elementOptions.data[BLOCK_NAME_DATA];

        fnOptions.data[ELEMENT_NAME_DATA] = blockName + '-' + name;

        if (elementOptions.hash.noWith) {
            value = elementOptions.fn(this, fnOptions);

        } else if (value) {
            value = elementOptions.fn(value, fnOptions);

        } else {
            value = elementOptions.inverse(value, fnOptions);
        }

        return new handlebars.SafeString(value);
    });

    var iframeJs = fs.readFileSync(path.join(__dirname, '..', 'node_modules/iframe-resizer/js/iframeResizer.contentWindow.min.js'), 'utf8');

    // if the example document contains a </head> then we append the iframe javascript to the <head>
    res.send(template({
        device: req.query.device === 'true',
        data: convert(data)
    }).replace(/\<\/head\>/, "<script>"+ iframeJs +"</script>\n</head>"));

    return true;
};
