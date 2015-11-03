module.exports = function (string) {
    return string.
            replace(/\W+/g, ' ').
            split(' ').
            map(function (part) { return part[0].toUpperCase() + part.slice(1); }).
            join(' ');
};
