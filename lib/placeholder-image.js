var GeoPattern = require('geopattern');
var XMLNode = require('geopattern/lib/xml')

module.exports = function () {
  return function (req, res, next) {
    var match = req.url.match(/^\/placeholder-image\/([^\/]+)\/(\d+)x(\d+)$/);

    if (!match) {
      return next();
    }

    // Geo pattern doesn't support arbitrary sizing,
    // so copy it into SVG pattern element.
    var geoPattern = GeoPattern.generate(match[1]);
    var root = geoPattern.svg.svg;
    var defs = new XMLNode('defs');
    var pattern = new XMLNode('pattern');

    pattern.children = root.children;
    root.children = [ ];

    pattern.setAttribute('id', 'pattern');
    pattern.setAttribute('patternUnits', 'userSpaceOnUse');
    pattern.setAttribute('width', root.attributes.width);
    pattern.setAttribute('height', root.attributes.height);
    root.appendChild(defs);
    defs.appendChild(pattern);

    // Resize SVG to requested size.
    var width = parseInt(match[2], 10);
    var height = parseInt(match[3], 10);

    root.setAttribute('width', width);
    root.setAttribute('height', height);

    // Fill SVG with the pattern.
    var rect = new XMLNode('rect');

    rect.setAttribute('x', 0);
    rect.setAttribute('y', 0);
    rect.setAttribute('width', width);
    rect.setAttribute('height', height);
    rect.setAttribute('fill', 'url(#pattern)');
    root.appendChild(rect);

    res.writeHead(200, { 'Content-Type': 'image/svg+xml' });
    res.end(geoPattern.toSvg());
  }
};
