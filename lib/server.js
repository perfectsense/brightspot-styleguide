var express = require('express');
var fs = require('fs');
var _ = require('lodash');
var md5 = require('md5');
var os = require('os');
var path = require('path');
var rrs = require('recursive-readdir-sync');
var traverse = require('traverse');
var url = require('url');
var xml2json = require('xml2json');

var label = require('./label');
var logger = require('./logger');
var renderPage = require('./render-page');

Array.prototype.toHTML = function () {
  var html = '';

  this.forEach(function (item) {
    html += item.toHTML ? item.toHTML() : item.toString();
  });

  return html;
};

var defaults = {
  'project-path': '.',
  'styleguide-path': 'styleguide',
  'json-suffix': '.json',

  'maven-pom': 'pom.xml',
  'maven-webapp': path.join('src', 'main', 'webapp'),

  host: 'localhost',
  port: 3000,

  devices: [
    {
      name: 'Mobile - Portrait',
      icon: 'fa fa-mobile',
      width: 320,
      height: 480
    },

    {
      name: 'Mobile - Landscape',
      icon: 'fa fa-mobile fa-rotate-270',
      width: 480,
      height: 320
    },

    {
      name: 'Tablet - Portrait',
      icon: 'fa fa-tablet',
      width: 768,
      height: 1024
    },

    {
      name: 'Tablet - Landscape',
      icon: 'fa fa-tablet fa-rotate-270',
      width: 1024,
      height: 768
    },

    {
      name: 'Desktop',
      icon: 'fa fa-desktop',
      width: 1200,
      height: 600
    }
  ]
};

module.exports = function (config) {
  logger.welcome();

  var app = express();

  app.use(require('./placeholder-image')());

  // Compile LESS on the fly.
  var cssCachePath = path.join(os.tmpDir(), 'styleguide-css-cache', md5(__dirname));

  app.use(require('less-middleware')(path.join(__dirname, '..', 'styleguide'), { dest: cssCachePath }));
  app.use(express.static(cssCachePath));

  // Custom fonts.
  app.use(require('connect-fonts').setup({
    fonts: [ require("connect-fonts-opensans") ]
  }));

  config = _.extend({ }, defaults, config);

  logger.success('Project Path: ' + config['project-path']);

  var styleguidePaths = config.styleguidePaths = [ ];
  var webappPaths = config.webappPaths = [ ];

  // Finds and adds styleguide path within base path.
  function addStyleguidePath(basePath) {
    var styleguidePath = path.join(basePath, config['styleguide-path']);

    if (fs.existsSync(styleguidePath)) {
      styleguidePaths.push(styleguidePath);
    }
  }

  // Finds and adds webapp path within base path.
  function addWebappPath(basePath) {
    var mavenPomPath = path.join(basePath, config['maven-pom']);

    if (fs.existsSync(mavenPomPath)) {
      var mavenWebappPath = path.join(basePath, config['maven-webapp']);

      function add(webappPath) {
        if (fs.existsSync(webappPath)) {
          webappPaths.push(webappPath);
        }
      }

      add(mavenWebappPath);

      var mavenPomXml = xml2json.toJson(fs.readFileSync(mavenPomPath), { object: true });
      var mavenTargetName = mavenPomXml.project.artifactId + '-' + mavenPomXml.project.version;

      add(path.join(config['project-path'], 'target', mavenTargetName));
    }
  }

  // Find the main styleguide and webapp paths.
  addStyleguidePath(config['project-path']);
  addWebappPath(config['project-path']);

  // Find the styleguide and webapp paths within direct NPM dependencies.
  var nodeModulesPath = path.join(config['project-path'], 'node_modules');

  fs.readdirSync(nodeModulesPath).forEach(function (module) {
    var modulePath = path.join(nodeModulesPath, module);

    addStyleguidePath(modulePath);
    addWebappPath(modulePath);
  });

  addStyleguidePath(path.join(__dirname, '..'));
  app.use('/node-modules', express.static(path.join(__dirname, '..', 'node_modules')));

  logger.success('Styleguide Paths: ' + styleguidePaths.join(', '));
  logger.success('Webapp Paths: ' + webappPaths.join(', '));

  styleguidePaths.find = function (filePath) {
    var prependedPaths = this.map(function (styleguidePath) {
      return path.join(styleguidePath, filePath);
    });

    return _.find(prependedPaths, function (prependedPath) {
      return fs.existsSync(prependedPath);
    });
  };

  // Main display.
  app.use(function (req, res, next) {
    var context = { };

    // Seed for the randomization.
    var seed = context.seed = parseInt(req.query.seed, 10) || Math.floor(Math.random() * 1000000);

    // Request URL (e.g. /foo/bar) to file path (e.g. \foo\bar in Windows).
    var requestedPath = context.requestedPath = path.join.apply(path, req.path.split('/'));

    // Example JSON data file?
    if (require('./example-file')(config, req, res, context)) {
      return;
    }

    // Styleguide names based on the request URL.
    var name = req.path.replace(/^\/|\/$/g, '');

    if (name.length > 0) {
      context.names = name.split('/').map(function (part) {
        return label(part);
      });
    }

    // Seed URL that pins the randomization.
    var seedUrl = url.parse(req.originalUrl, true);

    delete seedUrl.search;

    seedUrl.query.seed = seed;
    context.seedUrl = url.format(seedUrl);

    // Randomize URL that resets the randomization.
    if (req.query.seed) {
      var randomizeUrl = url.parse(req.originalUrl, true);

      delete randomizeUrl.search;
      delete randomizeUrl.query.seed;

      context.randomizeUrl = url.format(randomizeUrl);
    }

    // Finds all groups of examples in the main styleguide directory.
    var mainStyleguidePath = path.join(config['project-path'], config['styleguide-path']);
    var groups = context.groups = [ ];
    var originalUrlSearch = url.parse(req.originalUrl).search || '';

    fs.readdirSync(mainStyleguidePath).forEach(function (group) {
      if (group.slice(0, 1) !== '_') {
        var groupPath = path.join(mainStyleguidePath, group);

        if (fs.statSync(groupPath).isDirectory()) {
          var examples = [ ];

          // Finds all examples in this group.
          fs.readdirSync(groupPath).forEach(function (example) {
            if (example.slice(0, 1) !== '_') {
              var examplePath = path.join(groupPath, example);

              if (fs.statSync(examplePath).isDirectory()) {
                examples.push({
                  name: label(example),
                  url: '/' + group + '/' + example + originalUrlSearch
                });
              }
            }
          });

          if (examples.length > 0) {
            groups.push({
              name: label(group),
              examples: examples
            });
          }
        }
      }
    });

    // Which devices to display?
    context.resetDisplayDevicesUrl = url.parse(req.originalUrl, true);
    var availableDevices = context.availableDevices = [ ];
    var displayDevices = context.displayDevices = [ ];

    config.devices.forEach(function (device, i) {
      var key = 'd' + i;

      delete context.resetDisplayDevicesUrl.query[key];

      var deviceUrl = url.parse(req.originalUrl, true);
      var deviceQuery = deviceUrl.query;
      var availableDevice = {
        device: device
      };

      if (deviceQuery[key] === '1') {
        delete deviceQuery[key];
        availableDevice.selected = true;
        displayDevices.push({
          device: device
        });

      } else {
        deviceQuery[key] = '1';
      }

      delete deviceUrl.search;
      availableDevice.url = url.format(deviceUrl);
      availableDevices.push(availableDevice);
    });

    delete context.resetDisplayDevicesUrl.search;
    context.resetDisplayDevicesUrl = url.format(context.resetDisplayDevicesUrl);

    // LESS variables display?
    if (require('./less-variables')(config, req, res, context)) {
      return;
    }

    // Example directory with JSON data files within?
    var examplePath = styleguidePaths.find(requestedPath);

    if (!examplePath || !fs.statSync(examplePath).isDirectory()) {
      next();
      return;
    }

    // Finds all example files mapped to the request URL.
    var files = context.files = [ ];
    var mainFile;

    fs.readdirSync(examplePath).forEach(function (file) {
      if (file.slice(0, 1) !== '_' && path.extname(file) === config['json-suffix']) {
        var options = { };
        
        traverse(JSON.parse(fs.readFileSync(path.join(examplePath, file), 'utf8'))).forEach(function (value) {
          if (this.key === 'options' &&
              _.isPlainObject(value)) {

            var prefix = this.path.join('/') + '/';

            Object.keys(value).forEach(function (key) {
              options[prefix + key] = value[key];
            });
          }
        });

        var name = path.basename(file, config['json-suffix']);
        var f = {
          name: label(name),
          url: req.path + '/' + name,
          options: options
        };

        if (file === 'main.json') {
          mainFile = f;

        } else {
          files.push(f);
        }
      }
    });

    // Display the main example file first.
    if (mainFile) {
      files.unshift(mainFile);
    }

    // Render the main template.
    logger.success('URL: ' + req.originalUrl);
    res.send(renderPage(config, '/main.hbs', context));
    return;
  });

  styleguidePaths.forEach(function (styleguidePath) {
    app.use(express.static(styleguidePath));
  });

  webappPaths.forEach(function (webappPath) {
    app.use(express.static(webappPath));
  });

  // Start the web server.
  app.listen(config.port, config.host, function () {
    logger.success('Started on http://' + config.host + ':' + config.port);
  });
};
