var Chance = require('chance');
var _ = require('lodash');
var traverse = require('traverse');

function DataGenerator(context) {
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

function hex2rgb(hex,opacity){
    hex = hex.replace('#','');
    r = parseInt(hex.substring(0,2), 16);
    g = parseInt(hex.substring(2,4), 16);
    b = parseInt(hex.substring(4,6), 16);

    return 'rgba('+r+','+g+','+b+','+opacity/100+')';
}

function rgb2hex(rgba){
  var values = rgba
    .replace(/rgba?\(/, '')
    .replace(/\)/, '')
    .replace(/[\s+]/g, '')
    .split(',');
  var a = parseFloat(values[3] || 1),
      r = Math.floor(a * parseInt(values[0]) + (1 - a) * 255),
      g = Math.floor(a * parseInt(values[1]) + (1 - a) * 255),
      b = Math.floor(a * parseInt(values[2]) + (1 - a) * 255);
  return "#" + ("0" + r.toString(16)).slice(-2) + ("0" + g.toString(16)).slice(-2) + ("0" + b.toString(16)).slice(-2);
}

DataGenerator.prototype.hexcolor = function(brightness) {
    var color, hex, rgb;
    if(brightness) {
        var hex = this.chance.color({format: 'hex'});
            rgb = hex2rgb(hex, brightness);
            color = rgb2hex(rgb);

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
        return this.chance.bool({ likelihood: 5 }) ?
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
            node.update(value.replace(/\{\{(.*?)}}/, function (match, invocation) {
                if (invocation.indexOf('(') < 0) {
                    invocation += '()';
                }

                try {
                    return eval('self.' + invocation);

                } catch (err) {
                    throw new Error(invocation + ' is not a valid DataGenerator function invocation! ' + err.stack);
                }
            }));
        }
    });
};

module.exports = DataGenerator;
