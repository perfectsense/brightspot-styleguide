var fs = require('fs');
var handlebars = require('handlebars');
var less = require('less/lib/less-node');
var path = require('path');
var rrs = require('recursive-readdir-sync');
var traverse = require('traverse');

var label = require('./label');
var renderPage = require('./render-page');

module.exports = function render(config, req, res, context) {
    var match = context.requestedUrl.match(/^\/variables\/(.*)$/);

    if (!match) {
        return false;
    }

    // Display which variable file?
    var name = match[1];
    var selectedFilePath;

    context.library.findVariableFiles().forEach(function (varFile) {
        if (varFile.name === name) {
            selectedFilePath = varFile.path;
        }
    });

    //render icons by writing css from less file
    //source file: icon.less (ie - sibling of icon.vars)
    var iconPage = false;

    if(selectedFilePath.indexOf('icon') >= 0) {
        var src = selectedFilePath,
            lessFilePath = src.replace('.vars', '.less'),
            cssData;

        if (fs.existsSync(lessFilePath)) {
            //add css file to stylesheet ref in head
            var cssFilePath = lessFilePath.replace('.less', '-styles.css');
            context.iconFilePath = cssFilePath.replace('src/main/webapp', '');

            //read less file content and output to string
            less.render(fs.readFileSync(lessFilePath).toString(), {
                filename: path.resolve(lessFilePath),
            }, function(e, output) {
                cssData = output.css;
                //create css file and put in folder structure
                fs.writeFile(cssFilePath, cssData, function (err) {
                    if (err) {
                        return console.log('Not Writing to icon file: ' + err);
                    }
                });
            });

        } else {
            console.log('No '+ lessFilePath +' file found');
        }

        iconPage = true;
    }

    if (!selectedFilePath) {
        return false;
    }

    context.selectedFilePath = selectedFilePath;
    context.name = label(name);

    var items = context.items = [ ];

    function Variable() {
    }

    Variable.prototype.toHTML = function () {
        var templatePath = context.library.findStyleguideFile(path.join('variables', this.type + '.hbs'));

        if (!templatePath) {
            templatePath = context.library.findStyleguideFile(path.join('variables', 'default.hbs'));
        }

        var template = fs.readFileSync(templatePath, 'utf8');
        var compiledTemplate = handlebars.compile(template);

        return compiledTemplate(this);
    };

    var selectedFileData = fs.readFileSync(selectedFilePath, 'utf8');
    context.selectedFileData = selectedFileData;


    less.parse(selectedFileData, { filename: selectedFilePath }, function (err, root, imports, options) {
        if (err) {
            throw new Error(err);
        }

        traverse(root).forEach(function (node) {
            if (node) {
                var info = node.currentFileInfo;

                if (info && info.filename !== info.rootFilename) {
                    return;
                }
            }

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
                        if(iconPage) {
                            item.label = item.name;
                        } else {
                            item.label = label(item.name);
                        }

                        items.push(item);
                    }
                }

                return;
            }
        });

        // Create a fake ruleset for resolving all variables.
        var lessData = fs.readFileSync(selectedFilePath, 'utf8');
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

            res.send(renderPage(config, context, '/variables.hbs'));
        });
    });

    return true;
};
