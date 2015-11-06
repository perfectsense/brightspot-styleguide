var fs = require('fs');
var _ = require('lodash');
var path = require('path');
var rrs = require('recursive-readdir-sync');
var traverse = require('traverse');

var rootPath = process.argv[2] || 'styleguide';

rrs(rootPath).forEach(function (filePath) {
    if (filePath.slice(-5) === '.json') {
        var data = JSON.parse(fs.readFileSync(filePath, 'utf8'));

        // Add _template to attributes entry.
        traverse(data).forEach(function (value) {
            if (this.key === 'attributes'
                    && _.isPlainObject(value)
                    && !value._template) {

                value._template = 'common/attributes';
            }
        });

        // Remove empty options entry.
        traverse(data).forEach(function (value) {
            if (this.key === 'options'
                    && _.isPlainObject(value)
                    && _.isEmpty(value)) {

                this.remove();
            }
        });

        // Transform form inputs.
        traverse(data).forEach(function (value) {
            if (_.isPlainObject(value)
                    && value._template
                    && value._template.indexOf('components/bsp-form-') === 0) {

                // errors Array -> errorMessages Object.
                if (Array.isArray(value.errors)) {
                    value.errorMessages = {
                        _template: 'common/json-object'
                    };

                    value.errors.forEach(function (item) {
                        _.assign(value.errorMessages, item);
                    });

                    delete value.errors;
                }

                // label Object -> label and labelAttributes Object.
                var label = value.label;

                if (_.isPlainObject(label)) {
                    if (label.attributes) {
                        value.labelAttributes = label.attributes;
                        delete label.attributes;
                    }

                    if (!label._template) {
                        label._template = 'common/text';
                    }
                }
            }
        });

        // Flatten layout components.
        traverse(data).forEach(function (value) {
            if (_.isPlainObject(value)
                    && value._template
                    && value._template.indexOf('layouts/') === 0) {

                Object.keys(value).forEach(function (key) {
                    var v = value[key];

                    if (_.isPlainObject(v) && v.components) {
                        value[key] = v.components;
                    }
                });
            }
        });

        // Set promo item _template.
        function setPromoItemTemplate(template, itemsKey) {
            traverse(data).forEach(function (value) {
                if (_.isPlainObject(value)
                        && value._template === template
                        && value[itemsKey]) {

                    value[itemsKey].forEach(function (item) {
                        if (!item._template) {
                            item._template = template + '-item';
                        }
                    });
                }
            });
        }

        setPromoItemTemplate('components/bsp-image-promo', 'images');
        setPromoItemTemplate('components/bsp-list-promo', 'listItems');

        // Set CTA _template.
        traverse(data).forEach(function (value) {
            if (this.key === 'cta'
                    && _.isPlainObject(value)
                    && !value._template) {

                value._template = 'components/cta';
            }
        });

        // Add the proper prefix to _template entry.
        traverse(data).forEach(function (value) {
            if (this.key === '_template') {
                if (value.indexOf('bsp-') > -1 ||
                        fs.existsSync(path.join(rootPath, '..', 'src', 'main', 'webapp', 'render', value))) {

                    this.update('/render/' + value);

                } else {
                    this.update('/assets/templates/base/' + value);
                }
            }
        });

        fs.writeFileSync(filePath, JSON.stringify(data, null, '  ') + '\n', 'utf8');
    }
});
