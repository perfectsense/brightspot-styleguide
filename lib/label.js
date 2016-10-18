module.exports = function (string) {
    return string.
        replace(".json", "").
            replace(/\W+/g, ' ').
            trim().
            split(' ').
            map(function (part) { return part[0].toUpperCase() + part.slice(1); }).
            join(' ');
};
