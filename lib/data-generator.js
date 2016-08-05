var Chance = require('chance');
var _ = require('lodash');
var traverse = require('traverse');

var Util = require('./util');

function DataGenerator(context, config) {
    this.config = config;
    this.context = context;
    this.chance = new Chance(context.seed);
}

DataGenerator.prototype.stylesheet = function () {
    var stylesheets = this.context.availableStylesheets;
    if (stylesheets && stylesheets.length){
        // use the first stylesheet by default unless one has been selected
        var selectedSheet = sheet = stylesheets[0];
        for (var n=0, len=stylesheets.length; n<len; n++){
            sheet = stylesheets[n];
            if (sheet.selected) {
                selectedSheet = sheet;
                break;
            }
        }
        return selectedSheet.href;
    }
    throw new Error('Invalid! No stylesheet(s) were made available to the data');
};

DataGenerator.prototype.date = function (format) {
    var date;

    if(format == 'unformatted') {
        date = this.chance.date();
    } else if(format == 'short') {
        date = this.chance.date({string: true});
    } else if(format == 'iso') {
        var iso = this.chance.date({string: true});
            iso = iso.split("/");
            date = iso[2] + '-' + iso[0] + '-' + iso[1];
    } else {
        // in the format (July 23, 2075)
        var dateString, dateArray;
        dateString = this.chance.date();
        dateString = dateString.toString();
        dateString = dateString.substring(0,15);
        dateString = dateString.split(" ");

        for(i=1; i<4; i++){
            if(i == 1) {
                dateArray = Util.getfullMonth(dateString[i]);
            } else if(i == 2) {
                dateArray += dateString[i] + ', ';
            }
            else {
                dateArray += dateString[i] + '';
            }
        }
        date = dateArray;
    }
    return date;
};

DataGenerator.prototype.hexColor = function(luminosity) {
    var color, hex, rgb, hsl;

    luminosity = this.number(luminosity);

    if(luminosity === 0 || luminosity) {
        hex = this.chance.color({format: 'hex'});
        rgb = Util.hex2rgb(hex);
        hsl = Util.rgb2hsl(rgb[0], rgb[1], rgb[2]);
        rgb = Util.hsl2rgb(hsl[0], hsl[1], luminosity/100);
        color = Util.rgb2hex('rgba('+rgb[0]+', '+ rgb[1]+', '+ rgb[2]+')');
    } else {
        color = this.chance.color({format: 'hex'});
    }
    return color;
};

DataGenerator.prototype.image = function (width, height) {
    return '/placeholder-image/' + this.chance.guid() + '/' + this.number(width) + 'x' + this.number(height);
};

DataGenerator.prototype.name = function () {
    return this.chance.name();
};

DataGenerator.prototype.number = function (number) {
    if (Array.isArray(number)) {
        var step = number[2];
        number = this.chance.integer({
            min: number[0],
            max: number[1]
        });

        if (step) {
            number = Math.round(number / step) * step;
        }
    }
    else if (isNaN(number)) {
        return new Error('argument passed to number() is NaN. The `number()` generator expects you to provide a maximum number or range to produce a random result.\n')
    }
    else {
        number = this.chance.integer({
            min: 0,
            max: number
        });
    }

    return number;
};

DataGenerator.prototype._repeat = function (count, separator, callback) {
    var items = [ ];

    for (count = this.number(count); count > 0; -- count) {
        items.push(callback.call(this));
    }

    return items.join(separator)
};

function capitalize(string) {
    return string.length > 0 ? string.slice(0, 1).toUpperCase() + string.slice(1) : '';
}

DataGenerator.prototype.words = function (wordCount) {
    return capitalize(this._repeat(wordCount || [ 12, 18 ], ' ', function () {
        // when the wordcount is 1, only return a word.
        // otherwise, randomly choose between names or words (boosting words)
        return this.chance.bool({ likelihood: (wordCount === 1) ? 0 : 5 }) ?
            this.chance.name() :
            this.chance.word();
    }));
};

DataGenerator.prototype.sentences = function (sentenceCount, wordCount) {
    return this._repeat(sentenceCount || [ 3, 7 ], ' ', function () {
        return capitalize(this.words(wordCount)) + '.';
    });
};

DataGenerator.prototype.paragraphs = function (paragraphCount, sentenceCount, wordCount) {
    return this._repeat(paragraphCount || [ 1, 3 ], '', function () {
        return '<p>' + this.sentences(sentenceCount, wordCount) + '</p>';
    });
};

// `var` takes the provided key and looks up the value as defined in the `vars` of your styleguide config (_config.json)
// If it is found, it returns the corresponding value otherwise it returns an Error object
DataGenerator.prototype.var = function (key) {
    if (this.config.vars) {
        if (key in this.config.vars) {
            return this.config.vars[key];
        }
    }

    return new Error('this.config.vars is undefined. The `var()` generator expects that your styleguide _config.json contains vars. Or, maybe you misspelled the keyname?\n')
};

DataGenerator.prototype.process = function (data) {
    var self = this;

    traverse(data).forEach(function (value) {
        var node = this;

        // If there are any objects with _repeat entry in the list,
        // clone them.
        if (Array.isArray(value)) {
            var newArray = [ ];

            value.forEach(function (item) {
                var repeat = item._repeat;

                if (repeat) {
                    for (repeat = self.number(repeat); repeat > 0; -- repeat) {
                        newArray.push(_.clone(item, true));
                    }

                } else {
                    newArray.push(item);
                }
            });

            node.update(newArray);

        } else if (typeof value === 'string') {

            // Handlebars-like variable substitution.
            node.update(value.replace(/\{\{(.*?)}}/g, function (match, invocation) {
                if (invocation.indexOf('(') < 0) {
                    invocation += '()';
                }

                try {
                    var data = eval('self.' + invocation);
                    if (data instanceof Error) {
                        throw data;
                    }
                    else {
                        return data;
                    }
                } catch (err) {
                    throw new Error('DataGenerator execution error! ' + '\n\n' + err.stack);
                }
            }));
        }
    });
};

module.exports = DataGenerator;
