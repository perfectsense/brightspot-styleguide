var fs = require('fs');
var handlebars = require('handlebars');
var _ = require('lodash');

function read(context, filePath) {
    return fs.readFileSync(context.library.findStyleguideFile(filePath), 'utf8');
}

module.exports = function (config, context, templatePath) {
    handlebars.registerPartial('layout', read(context, '/layout.hbs'));

    var template = handlebars.compile(read(context, templatePath), {
        preventIndent: true
    });

    return template(context);
};
