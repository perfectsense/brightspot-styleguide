var fs = require('fs');
var handlebars = require('handlebars');
var _ = require('lodash');
var path = require('path');

function read(filePath) {
    return fs.readFileSync(path.join(__dirname, filePath), 'utf8');
}

module.exports = function (config, context, templatePath) {
    handlebars.registerPartial('layout', read('/layout.hbs'));

    if (config.example){
        context.example = config.example;
    }

    var template = handlebars.compile(read(templatePath), {
        preventIndent: true
    });

    return template(context);
};
