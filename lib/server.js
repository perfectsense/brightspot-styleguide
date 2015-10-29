var cors = require('cors');
var express = require('express');
var fs = require('fs');
var handlebars = require('handlebars');
var _ = require('lodash');
var path = require('path');
var traverse = require('traverse');
var url = require('url');
var xml2json = require('xml2json');

var DataGenerator = require('./data-generator');
var hbsRenderer = require('./handlebars-renderer');
var label = require('./label');
var logger = require('./logger');
var placeholderImage = require('./placeholder-image');

Array.prototype.toHTML = function () {
  var html = '';

  this.forEach(function (item) {
    html += item.toHTML ? item.toHTML() : item.toString();
  });

  return html;
};

function Template() {
}

Template.prototype.toHTML = function () {
  var partial = handlebars.partials[this._template];
  var template = handlebars.compile(partial);

  return template(this);
};

var defaults = {
  'project-path': '.',
  'styleguide-path': 'styleguide',
  'json-suffix': '.json',

  'maven-pom': 'pom.xml',
  'maven-webapp': path.join('src', 'main', 'webapp'),

  host: 'localhost',
  port: 3000,
  baseUrl: '/styleguide',

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

  app.set('env', 'development');
  app.use(cors());
  app.use(placeholderImage());

  config = _.extend({ }, defaults, config);

  logger.success('Project Path: ' + config['project-path']);

  var styleguidePaths = [ ];
  var templatePaths = [ ];

  // Finds and adds styleguide path within base path.
  function addStyleguidePath(basePath) {
    var styleguidePath = path.join(basePath, config['styleguide-path']);

    if (fs.existsSync(styleguidePath)) {
      app.use('/', express.static(styleguidePath));
      styleguidePaths.push(styleguidePath);
    }
  }

  // Finds and adds tempate path within base path.
  function addTemplatePath(basePath) {
    var mavenPomPath = path.join(basePath, config['maven-pom']);

    if (fs.existsSync(mavenPomPath)) {
      var mavenWebappPath = path.join(basePath, config['maven-webapp']);

      function add(templatePath) {
        if (fs.existsSync(templatePath)) {
          app.use('/', express.static(templatePath));
          templatePaths.push(path.join(templatePath, 'render'));
        }
      }

      add(mavenWebappPath);

      var mavenPomXml = xml2json.toJson(fs.readFileSync(mavenPomPath), { object: true });
      var mavenTargetName = mavenPomXml.project.artifactId + '-' + mavenPomXml.project.version;

      add(path.join(config['project-path'], 'target', mavenTargetName));
    }
  }

  // Find the main styleguide and template paths.
  addStyleguidePath(config['project-path']);
  addTemplatePath(config['project-path']);

  // Find the styleguide and template paths within direct NPM dependencies.
  var nodeModulesPath = path.join(config['project-path'], 'node_modules');

  fs.readdirSync(nodeModulesPath).forEach(function (module) {
    var modulePath = path.join(nodeModulesPath, module);

    addStyleguidePath(modulePath);
    addTemplatePath(modulePath);
  });

  addStyleguidePath(path.join(__dirname, '..'));
  app.use(config.baseUrl + '/dev', express.static(path.join(__dirname, '..', 'node_modules')));

  hbsRenderer.init({
    styleguidePaths: styleguidePaths,
    templatePaths: templatePaths
  });

  logger.success('Styleguide Paths: ' + styleguidePaths.join(', '));
  logger.success('Template Paths: ' + templatePaths.join(', '));

  function findInStyleguide(filePath) {
    var prependedPaths = styleguidePaths.map(function (styleguidePath) {
      return path.join(styleguidePath, filePath);
    });

    return _.find(prependedPaths, function (prependedPath) {
      return fs.existsSync(prependedPath);
    });
  }

  // Main display.
  app.use(config.baseUrl, function (req, res) {
    logger.success('URL: ' + req.originalUrl);
    hbsRenderer.registerPartials();

    var context = { };

    // Seed for the randomization.
    var requestedSeed = parseInt(req.query.seed, 10);
    var seed = context.seed = requestedSeed || Math.floor(Math.random() * 1000000);

    // Request URL (e.g. /foo/bar) to file path (e.g. \foo\bar in Windows).
    var requestedPath = path.join.apply(path, req.path.split('/'));

    // Example JSON data file?
    var filePath = findInStyleguide(requestedPath + '.json');

    if (filePath) {
      var data = JSON.parse(fs.readFileSync(filePath, 'utf8'));
      var currentDataPath = filePath;

      traverse(data).forEach(function (value) {
        var dataUrl = value._dataUrl;

        if (dataUrl) {
          var dataPath = path.join.apply(path, dataUrl.split('/'));

          if (dataUrl.slice(0, 1) === '/') {
            dataPath = findInStyleguide(dataPath);

            if (!dataPath) {
              throw new Error("Can't find " + dataUrl + " in any of the styleguide directories!");
            }

          } else {
            dataPath = path.resolve(currentDataPath, '..', dataPath);

            if (!dataPath) {
              throw new Error("Can't find " + dataUrl + " relative to " + currentDataPath + "!");
            }
          }

          var originalDataPath = currentDataPath;
          currentDataPath = dataPath;

          this.update(JSON.parse(fs.readFileSync(dataPath, 'utf8')));

          currentDataPath = originalDataPath;
        }
      });

      // Validate the JSON data.
      traverse(data).forEach(function (value) {
        if (_.isPlainObject(value)
            && !value._template
            && this.key !== 'options') {

          throw new Error("Object without _template entry at " + this.path.join('/') + "!");
        }
      });

      // Randomize the JSON data.
      new DataGenerator(seed).process(data);

      function convert(data) {
        if (typeof data === 'object') {
          if (Array.isArray(data)) {
            return data.map(function (item) {
              return convert(item);
            });

          } else {
            var copy = data._template ? new Template() : {};

            Object.keys(data).forEach(function (key) {
              copy[key] = convert(data[key]);
            });

            return copy;
          }
        }

        return data;
      }

      var template = handlebars.compile(fs.readFileSync(findInStyleguide('/iframe.hbs'), 'utf8'));

      // if we have a body template, we do need the iframe.hbs to drop us a body tag
      // as it means we are creating a full styleguide page, which has it's own body renderer
      if(data._template === 'common/body') {
        data.body = false;
      } else {
        data.body = true;
      }

      return res.send(template({
        device: req.query.device === 'true',
        data: convert(data)
      }));
    }

    // Example directory with JSON data files within?
    var examplePath = findInStyleguide(requestedPath);

    if (!examplePath || !fs.statSync(examplePath).isDirectory()) {
      return res.end(404);
    }

    // Styleguide names based on the request URL.
    var name = req.path.slice(config.baseUrl.length);

    if (name.slice(0, 1) === '/') {
      name = name.slice(1);
    }

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

    // Finds all example files mapped to the request URL.
    var files = context.files = [ ];
    var mainFile;

    fs.readdirSync(examplePath).forEach(function (file) {
      if (file.slice(0, 1) !== '_' && path.extname(file) === config['json-suffix']) {
        var name = path.basename(file, config['json-suffix']);
        var f = {
          name: label(name),
          url: config.baseUrl + req.path + '/' + name
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
                  url: config.baseUrl + '/' + group + '/' + example + originalUrlSearch
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

    // Render the main template.
    var template = handlebars.compile(fs.readFileSync(findInStyleguide('/main.hbs'), 'utf8'));

    return res.send(template(context));
  });

  // Start the web server.
  app.listen(config.port, config.host, function () {
    logger.success('Started on http://' + config.host + ':' + config.port + config.baseUrl);
  });
};
