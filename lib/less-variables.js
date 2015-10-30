var fs = require('fs');
var path = require('path');
var rrs = require('recursive-readdir-sync');
var traverse = require('traverse');

var renderPage = require('./render-page');

module.exports = function (config, req, res, context) {
  if (req.url.indexOf('/less-variables') !== 0) {
    return false;
  }

  var lessVariables = context.lessVariables = [ ];

  config.webappPaths.forEach(function (webappPath) {
    if (webappPath.indexOf('/target/') > 0) {
      return;
    }

    rrs(webappPath).forEach(function (filePath) {
      if (path.extname(filePath) !== '.less') {
        return;
      }

      var file = fs.readFileSync(filePath, 'utf8');
      var varPattern = /^(@\S+)\s*:\s*(.+)\s*;\s*$/gm;
      var varMatch;

      while (varMatch = varPattern.exec(file)) {
        var name = varMatch[1];

        // Exclude Font Awesome variables.
        if (name.indexOf('@fa-') === 0) {
          continue;
        }

        lessVariables.push({
          name: name,
          value: varMatch[2],
          path: filePath
        });
      }
    });
  });

  lessVariables.sort(function (x, y) {
    return x.name.localeCompare(y.name);
  });

  res.send(renderPage(config, '/less-variables.hbs', context));
  return;
};
