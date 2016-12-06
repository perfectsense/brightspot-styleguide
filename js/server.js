const express = require('express')
const opn = require('opn')

const logger = require('./logger')

module.exports = config => {
  logger.welcome()

  Object.keys(config).sort().forEach(key => {
    logger.info(`Config: ${key}: ${JSON.stringify(config[key])}`)
  })

  const app = express()

  app.use(require('./placeholder-image')())
  app.use(express.static(config.build))

  const server = app.listen(config.port, config.host, () => {
    const url = `http://${config.host}:${config.port}/styleguide.html`

    logger.success(`Server started on ${url}`)
    opn(url)
  })

  server.on('error', function (error) {
    if (error.errno === 'EADDRINUSE') {
      logger.error(`Another server already running on ${config.port}!`)

    } else {
      logger.error(`Unknown error! ${error}`)
    }

    this.close()
    process.exit(0)
  })
}
