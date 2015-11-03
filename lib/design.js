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
  var variableFilePaths = [ ];

  rrs(config.webappPaths[0]).forEach(function (filePath) {
    if (path.basename(filePath) === name + '.vars') {
      variableFilePaths.push(filePath);
    }
  });

  if (variableFilePaths.length > 0) {
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

    variableFilePaths.forEach(function (variableFilePath) {
      less.parse(fs.readFileSync(variableFilePath, 'utf8'), { filename: variableFilePath }, function (err, root, imports, options) {
        traverse(root).forEach(function (node) {

          // Documentation comment.
          if (node instanceof less.tree.Comment) {
            if (!node.isLineComment) {
              var text = node.value;
              var item = new Variable();

              text = text.replace(/^\/[\n\r\s*]+/m, '');
              text = text.replace(/[\n\r\s*]+\/$/m, '');
              text = text.replace(/([\n\r]\s*)\*+/g, '$1');
              text = handlebars.escapeExpression(text);
              text = '<p>' + text.replace(/(\s*[\n\r]\s*){2,}/g, '</p><p>') + '</p>';

              item.description = new handlebars.SafeString(text);

              items.push(item);
            }

            return;
          }

          // Variable declaration.
          if (node instanceof less.tree.Rule) {
            var nodeName = node.name

            if (typeof nodeName === 'string' && nodeName.indexOf('@') === 0) {
              nodeName = nodeName.slice(1);
              var dashAt = nodeName.indexOf('-');

              if (dashAt > -1) {
                var item = new Variable();
                item.type = nodeName.slice(0, dashAt);
                item.label = label(nodeName.slice(dashAt + 1));
                item.name = nodeName.slice(dashAt + 1);

                // go through the Rule object and convert it to CSS
                item.value = node.value.toCSS();

                // if a deep object (HACKY!) contains a name, it means there is a variable being used
                // in the value of that CSS rule. So add the variable to the value
                node.value.value[0].value.forEach(function(variable) {
                  if(variable.name) {
                    item.value += variable.name;
                  }
                });

                items.push(item);
              }
            }

            return;
          }
        });
      });
    });

    res.send(renderPage(config, '/design.hbs', context));
    return true;
  }

  return false;
};
