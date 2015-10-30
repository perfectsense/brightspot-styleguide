var fs = require('fs');
var handlebars = require('handlebars');

function read(config, filePath) {
  return fs.readFileSync(config.styleguidePaths.find(filePath), 'utf8');
}

module.exports = function (config, templatePath, context) {
  handlebars.registerPartial('layout', read(config, '/layout.hbs'));

  var template = handlebars.compile(read(config, templatePath));

  return template(context);
};
