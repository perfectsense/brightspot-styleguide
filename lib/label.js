module.exports = function (string) {
    return string.
            replace(/\W+/g, ' ').
            trim().
            split(' ').
            map(function (part) {
                if (part.length > 0) {
                    return part[0].toUpperCase() + part.slice(1);
                }
                else {
                    return part;
                }
            }).
            join(' ');
};
