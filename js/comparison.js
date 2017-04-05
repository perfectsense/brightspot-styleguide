const fs = require('fs')
const handlebars = require('handlebars')
const path = require('path')

const resolver = require('./resolver')

module.exports = function (styleguide, filePath) {
  const buildPath = styleguide.path.build()
  let data = resolver.data(buildPath, filePath)

  function renderTemplate (data) {
    const template = handlebars.compile(fs.readFileSync(path.join(__dirname, 'comparison.hbs'), 'utf8'))
    return template(data)
  }

  return {
    html: renderTemplate(data)
  }
}
