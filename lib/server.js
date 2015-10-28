var cors = require('cors');
var express = require('express');
var fs = require('fs');
var hbs = require('handlebars');
var _ = require('lodash');
var path = require('path');
var traverse = require('traverse');
var xml2json = require('xml2json');

var DataGenerator = require('./data-generator');
var hbsRenderer = require('./handlebars-renderer');
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
  var partial = hbs.partials[this._template];
  var template = hbs.compile(partial);

  return template(this);
};

var defaults = {
  host: 'localhost',
  'maven-pom': 'pom.xml',
  'maven-webapp': path.join('src', 'main', 'webapp'),
  port: 3000,
  project: '.',
  styleguide: 'styleguide'
};

module.exports = function (config) {
  logger.welcome();

  var app = express();

  app.set('env', 'development');
  app.use(cors());
  app.use(placeholderImage());

  //
  config = _.extend({ }, defaults, config);

  logger.success('Project: ' + config.project);

  // Find the main styleguide.
  var styleguidePaths = [ ];
  var templatePaths = [ ];

  function findStyleguidePath(basePath) {
    var styleguidePath = path.join(basePath, config.styleguide);

    if (fs.existsSync(styleguidePath)) {
      app.use('/', express.static(styleguidePath));
      styleguidePaths.push(styleguidePath);
    }
  }

  function addTemplatePath(templatePath) {
    if (fs.existsSync(templatePath)) {
      app.use('/', express.static(templatePath));
      templatePaths.push(path.join(templatePath, 'render'));
    }
  }

  function findTemplatePath(basePath) {
    var mavenPomPath = path.join(basePath, config['maven-pom']);

    if (fs.existsSync(mavenPomPath)) {
      var mavenWebappPath = path.join(basePath, config['maven-webapp']);

      addTemplatePath(mavenWebappPath);

      var mavenPomXml = xml2json.toJson(fs.readFileSync(mavenPomPath), { object: true });
      var mavenTargetName = mavenPomXml.project.artifactId + '-' + mavenPomXml.project.version;

      addTemplatePath(path.join(config.project, 'target', mavenTargetName));
    }
  }

  // Find the main styleguide and template path.
  findStyleguidePath(config.project);
  findTemplatePath(config.project);

  // Find the styleguide and template paths within direct NPM dependencies.
  var nodeModulesPath = path.join(config.project, 'node_modules');

  fs.readdirSync(nodeModulesPath).forEach(function (module) {
    var modulePath = path.join(nodeModulesPath, module);

    findStyleguidePath(modulePath);
    findTemplatePath(modulePath);
  });

  findStyleguidePath(path.join(__dirname, '..'));
  app.use('/styleguide/normalize.css', express.static(path.join(__dirname, '..', 'node_modules', 'normalize.css', 'normalize.css')));
  app.use('/styleguide/less.min.js', express.static(path.join(__dirname, '..', 'node_modules', 'less', 'dist', 'less.min.js')));
  app.use('/styleguide/jquery.min.js', express.static(path.join(__dirname, '..', 'node_modules', 'jquery', 'dist', 'jquery.min.js')));

  hbsRenderer.init({
    styleguidePaths: styleguidePaths,
    templatePaths: templatePaths
  });

  function findHandlebarsTemplate(file) {
    for (var i = 0, len = styleguidePaths.length; i < len; ++ i) {
      var filePath = path.join(styleguidePaths[i], file);

      if (fs.existsSync(filePath)) {
        return fs.readFileSync(filePath, 'utf8');
      }
    }

    throw new Error(file + " doesn't exist!");
  }

  app.use('/styleguide', function (req, res) {
    var requestedSeed = parseInt(req.query.seed, 10);
    var seed = requestedSeed || Math.floor(Math.random() * 1000000);

    hbsRenderer.registerPartials();

    var requestedPath = path.join(config.styleguide, req.path);
    var examplePath = requestedPath + '.json';

    if (fs.existsSync(examplePath)) {
      var data = hbsRenderer.getJSONData(examplePath.slice(config.styleguide.length));

      traverse(data).forEach(function (value) {
        var dataUrl = value._dataUrl;

        if (dataUrl) {
          this.update(hbsRenderer.getJSONData(dataUrl));
        }
      });

      traverse(data).forEach(function (value) {
        if (typeof value === 'object'
            && !Array.isArray(value)
            && !value._template
            && this.key !== 'options') {

          throw new Error("Object without _template entry at " + this.path.join('/') + "!");
        }
      });

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

      var template = hbs.compile(findHandlebarsTemplate('iframe.hbs'));

      return res.send(template(convert(data)));
    }

    if (!fs.statSync(requestedPath).isDirectory()) {
      return res.end(404);
    }

    var name = requestedPath.slice(config.styleguide.length);

    if (name.slice(0, 1) === '/') {
      name = name.slice(1);
    }

    var files = [ ];
    var indexFile;

    fs.readdirSync(requestedPath).forEach(function (file) {
      if (file.slice(0, 1) !== '_' && file.slice(-5) === '.json') {
        var name = file.slice(0, -5);
        var filePath = '/' + requestedPath + '/' + name;

        if (file === 'index.json') {
          indexFile = {
            name: name,
            path: filePath
          };

        } else {
          files.push({
            name: name,
            path: filePath
          });
        }
      }
    });

    if (indexFile) {
      files.unshift(indexFile);
    }

    var groups = [ ];

    fs.readdirSync(config.styleguide).forEach(function (group) {
      if (group.slice(0, 1) !== '_') {
        var groupPath = path.join(config.styleguide, group);

        if (fs.statSync(groupPath).isDirectory()) {
          var examples = [ ];

          fs.readdirSync(groupPath).forEach(function (example) {
            if (example.slice(0, 1) !== '_') {
              var examplePath = path.join(groupPath, example);

              if (fs.statSync(examplePath).isDirectory()) {
                examples.push({
                  name: example,
                  path: '/' + examplePath
                });
              }
            }
          });

          if (examples.length > 0) {
            groups.push({
              name: group,
              examples: examples
            });
          }
        }
      }
    });

    var template = hbs.compile(findHandlebarsTemplate('main.hbs'));

    return res.send(template({
      requestedSeed: requestedSeed,
      seed: seed,
      name: name,
      groups: groups,
      files: files
    }));
  });

  logger.success('Styleguides: ' + styleguidePaths.join(', '));
  logger.success('Templates: ' + templatePaths.join(', '));

  // Start the web server.
  app.listen(config.port, config.host, function () {
    logger.success('Started on ' + config.host + ':' + config.port);
  });
};
