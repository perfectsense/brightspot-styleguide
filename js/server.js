var bodyParser = require('body-parser')
var exec = require('child_process').exec
var express = require('express')
var fs = require('fs')
var _ = require('lodash')
var marked = require('marked')
var path = require('path')
var sentenceCase = require('sentence-case')
var traverse = require('traverse')
var url = require('url')
var querystring = require('querystring')

var label = require('./label')
var Project = require('./project')
var logger = require('./logger')
var renderPage = require('./render-page')

var defaults = {
  'project-path': '.',
  'project-src-path': path.join('src', 'main', 'webapp'),
  'styleguide-path': 'styleguide',
  'json-suffix': '.json'
}

module.exports = function (config) {
  config = _.extend({ }, defaults, config)

  logger.welcome()

  let app = express()

  app.use(bodyParser.urlencoded({ extended: true }))

    // Automatically generated placeholder images.
  app.use(require('./placeholder-image')())

  var project = config.project = new Project(config, config['project-path'])

  logger.success('Project: ' + project.name)
  logger.success(` \u{1F539} Path: ${config['project-path']}`)
  logger.success(` \u{1F539} Source Path: ${config['project-src-path']}`)
  logger.success(` \u{1F539} build: ${config['build']}`)

    // Main display.
  app.use(function (req, res, next) {
    var context = { }

    context.project = project

        // Request URL (e.g. /foo/bar) to file path (e.g. \foo\bar in Windows).
    var requestedUrl = req.path

    context.requestedUrl = requestedUrl

    var requestedPath = context.requestedPath = path.join.apply(path, requestedUrl.split('/'))

        // Styleguide names based on the request URL.
    var name = req.path.replace(/^\/|\/$/g, '')

    if (name.length > 0) {
      context.names = name.split('/').map(function (part) {
        return label(part)
      })
    }

    var originalUrlSearch = context.originalUrlSearch = url.parse(req.originalUrl).search || ''

    context.reqPath = req.path
    context.contentQueryString = querystring.stringify(req.query) || ''

    var examples = context.examples = [ ]
    var groupNavigation = context.groupNavigation = [ ]
        // Finds all groups of examples in the styleguide directory.

    project.forEachStyleguideItem(function (group, groupPath) {
      var oldDirectory = null

      function traverseDirectory (dirPath, groupName) {
        var name = path.basename(dirPath)
        var item = {}
        var group = {}

        if (name.slice(0, 1) !== '_') {
          var urlPath = dirPath.split(project._targetPath).pop()

          if (path.extname(urlPath) === config['json-suffix']) {
                        // item.name = label(name);
            item = path.parse(urlPath)
            item.url = path.join(item.dir, item.name) + originalUrlSearch
            item.name = label(sentenceCase(item.name))

            let formatedLabel = (item.dir.charAt(0) === '/') ? item.dir.substr(1).replace(/\//g, ' / ') : item.dir.replace(/\//g, ' / ')
            // if formatedLabel is empty set to item.label
            formatedLabel = formatedLabel || item.name
            item.dirName = label(formatedLabel, /[^a-zA-Z0-9_/]/g)

            if (oldDirectory !== item.dir) {
              examples.push(group)
              group.name = item.dirName
              groupNavigation = []
            }
                        // if the json file is at the root object will not have children.
                        // push item directly to examples
            if (groupName) {
              examples.pop()
              examples.push(item)
            } else {
              groupNavigation.push(item)
              group.children = groupNavigation
            }

            oldDirectory = item.dir
          } else if (fs.statSync(dirPath).isDirectory()) {
            try {
                            // if its a directory delete the url so that it doesn't genearate a clickable path.
              fs.readdirSync(dirPath)
                                .forEach(child => traverseDirectory(path.join(dirPath, child)))
                                .filter(e => !!e)
            } catch (ex) {
              if (ex.code === 'EACCES') {
                return null
              }
            }
          } else {
            return null // Or set item.size = 0 for devices, FIFO and sockets ?
          }
          return item
        }
      }
      traverseDirectory(groupPath, group)
    })

        // Which devices to display?
    context.resetDisplayDevicesUrl = url.parse(req.originalUrl, true)
    var availableDevices = context.availableDevices = [ ]
    var displayDevices = context.displayDevices = [ ]

    config.devices.forEach(function (device, i) {
      var key = 'd' + i

      delete context.resetDisplayDevicesUrl.query[key]

      var deviceUrl = url.parse(req.originalUrl, true)
      var deviceQuery = deviceUrl.query
      var availableDevice = {
        device: device
      }

      if (deviceQuery[key] === '1') {
        delete deviceQuery[key]
        availableDevice.selected = true
        displayDevices.push({
          device: device
        })
      } else {
        deviceQuery[key] = '1'
      }

      delete deviceUrl.search
      availableDevice.url = url.format(deviceUrl)
      availableDevices.push(availableDevice)
    })

    delete context.resetDisplayDevicesUrl.search
    context.resetDisplayDevicesUrl = url.format(context.resetDisplayDevicesUrl)

        // Example directory with JSON data files within?
    var examplePath = project.findStyleguideFile(requestedPath)

        // res.send(renderContent(config, context, '/main.hbs'));
    if (require('./render-content')(config, req, res, context)) {
      return
    }

        // Example JSON data file?
    if (require('./example-file')(config, req, res, context)) {
      return
    }

        // Not a directory or a styleguide route? (ie: jquery.js)
    if (!examplePath || !fs.statSync(examplePath).isDirectory()) {
      next()
      return
    }

        // Render the main template.
    logger.success('URL: ' + req.originalUrl)
    return res.send(renderPage(config, context, '/main.hbs'))
  })

  app.use('/node-modules', (req, res, next) => {
    let absUrl = require.resolve(req.url.replace(/^\/+/g, ''))
    res.sendFile(absUrl)
  })

  app.use(express.static(config.build))

    // Start the web server.
  let server = app.listen(config.port, config.host, function () {
    logger.success(`Started on http://${config.host}:${config.port}`)
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
