const fs = require('fs-extra')
const GeoPattern = require('geopattern')
const XMLNode = require('geopattern/lib/xml')
const path = require('path')

module.exports = function (key, width, height, file) {
  // Geo pattern doesn't support arbitrary sizing,
  // so copy it into SVG pattern element.
  const geoPattern = GeoPattern.generate(key)
  const root = geoPattern.svg.svg
  const defs = new XMLNode('defs')
  const pattern = new XMLNode('pattern')

  pattern.children = root.children
  root.children = [ ]

  pattern.setAttribute('id', 'pattern')
  pattern.setAttribute('patternUnits', 'userSpaceOnUse')
  pattern.setAttribute('width', root.attributes.width)
  pattern.setAttribute('height', root.attributes.height)
  root.appendChild(defs)
  defs.appendChild(pattern)

  // Resize SVG to requested size.
  root.setAttribute('width', width)
  root.setAttribute('height', height)

  // for IE 9/10/11 scalability of SVGs
  root.setAttribute('viewBox', '0 0 ' + width + ' ' + height)

  // Fill SVG with the pattern.
  const rect = new XMLNode('rect')

  rect.setAttribute('x', 0)
  rect.setAttribute('y', 0)
  rect.setAttribute('width', width)
  rect.setAttribute('height', height)
  rect.setAttribute('fill', 'url(#pattern)')
  root.appendChild(rect)

  fs.mkdirsSync(path.dirname(file))
  fs.writeFileSync(file, geoPattern.toSvg())
}
