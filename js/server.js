const express = require('express')
const opn = require('opn')

const Project = require('./project')
const logger = require('./logger')

module.exports = function (config) {
  logger.welcome()

  const app = express()

    // Automatically generated placeholder images.
  app.use(require('./placeholder-image')())

  var project = config.project = new Project(config, config.root)

  logger.success('Project: ' + project.name)
  logger.success(` \u{1F539} root: ${config.root}`)
  logger.success(` \u{1F539} source: ${config.source}`)
  logger.success(` \u{1F539} build: ${config.build}`)

  app.use('/node-modules', (req, res, next) => {
    let absUrl = require.resolve(req.url.replace(/^\/+/g, ''))
    res.sendFile(absUrl)
  })

  app.use(express.static(config.build))

  const server = app.listen(config.port, config.host, () => {
    const url = `http://${config.host}:${config.port}/styleguide.html`
    logger.success(`Started on ${url}`)
    opn(url)
  })

    // Handle server errors
  server.on('error', function (err) {
    if (err.errno === 'EADDRINUSE') {
      logger.warn(`We tried starting the styleguide on ${config.host}:${config.port}, but it's already being used. Maybe you've got another styleguide running?`)
    } else {
      console.log(err)
    }

    this.close()
    process.exit(0)
  })
}
