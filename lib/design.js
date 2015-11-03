var fs = require('fs');
var handlebars = require('handlebars');
var less = require('less/lib/less-node');
var path = require('path');
var rrs = require('recursive-readdir-sync');
var traverse = require('traverse');

var label = require('./label');
var renderPage = require('./render-page');

module.exports = function render(config, req, res, context) {
    var match = req.url.match(/^\/design\/(.*)$/);

    if (!match) {
        return false;
    }

    var name = match[1];
    var lessData = '';
    var selectedFilePath;

    rrs(config.webappPaths[0]).forEach(function (filePath) {
        if (path.extname(filePath) === '.vars') {
            lessData += fs.readFileSync(filePath, 'utf8');
            lessData += '\n';

            if (path.basename(filePath, '.vars') === name) {
                selectedFilePath = filePath;
            }
        }
    });

    if (!selectedFilePath) {
        return false;
    }

    context.name = label(name);
    var items = context.items = [ ];

    function Variable() {
    }

    Variable.prototype.toHTML = function () {
        var templatePath = config.styleguidePaths.find(path.join('design', this.type + '.hbs'));

        if (!templatePath) {
            templatePath = config.styleguidePaths.find(path.join('design', 'default.hbs'));
        }

        var template = fs.readFileSync(templatePath, 'utf8');
        var compiledTemplate = handlebars.compile(template);

        return compiledTemplate(this);
    };

    less.parse(fs.readFileSync(selectedFilePath, 'utf8'), { filename: selectedFilePath }, function (err, root, imports, options) {
        if (err) {
            throw new Error(err);
        }

        traverse(root).forEach(function (node) {

            // Documentation comment.
            if (node instanceof less.tree.Comment) {
                if (!node.isLineComment) {
                    var text = node.value;

                    text = text.replace(/-+/g, ''); // get rid of long divider lines
                    text = text.replace(/^\/[\n\r\s*]+/m, '');
                    text = text.replace(/[\n\r\s*]+\/$/m, '');
                    text = text.replace(/([\n\r]\s*)\*+/g, '$1');
                    text = handlebars.escapeExpression(text);
                    text = '<p>' + text.replace(/(\s*[\n\r]\s*){2,}/g, '</p><p>') + '</p>';

                    var item = new Variable();
                    item.description = new handlebars.SafeString(text);

                    items.push(item);
                }

                return;
            }

            // Variable declaration.
            if (node instanceof less.tree.Rule) {
                var nodeName = node.name

                if (typeof nodeName === 'string' && nodeName.indexOf('@') === 0 && nodeName.indexOf('_') !== 1 ) {
                    nodeName = nodeName.slice(1);
                    var dashAt = nodeName.indexOf('-');

                    if (dashAt > -1) {
                        var item = new Variable();
                        item.node = node;
                        item.type = nodeName.slice(0, dashAt);
                        item.name = nodeName.slice(dashAt + 1);
                        item.label = label(item.name);

                        items.push(item);
                    }
                }

                return;
            }
        });

        // Create a fake ruleset for resolving all variables.
        lessData += '.brightspot-styleguide{';

        context.items.forEach(function (item) {
            if (item.name) {
                lessData += 'brightspot-styleguide-';
                lessData += item.name;
                lessData += ':';
                lessData += item.node.name;
                lessData += ';\n';
            }
        });

        lessData += '}';

        less.render(lessData, { filename: selectedFilePath }, function (err, result) {
            if (err) {
                throw new Error(err);
            }

            context.items.forEach(function (item) {
                if (item.name) {
                    var match = result.css.match(new RegExp('brightspot-styleguide-' + item.name + ':([^;]*);'));

                    if (match) {
                        item.value = match[1];
                    }
                }
            });

            res.send(renderPage(config, '/design.hbs', context));
        });
    });

    return true;
};
