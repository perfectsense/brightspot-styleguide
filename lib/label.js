module.exports = function  (string, regExpression) {
    label = string.
        replace('.json', '').
        replace( (regExpression) ? regExpression : /\W+/g, ' ').
        trim().
        split(' ').
        map(function (part) { return part[0].toUpperCase() + part.slice(1); }).
        join(' ');

        if (label === 'Index'){
            label = 'Main';
        }

    return label;
};
