var bodyParser = require('body-parser');
var exec = require('child_process').exec;
var express = require('express');
var fs = require('fs');
var _ = require('lodash');
var marked = require('marked');
var md5 = require('md5');
var os = require('os');
var path = require('path');
var sentenceCase = require('sentence-case')
var traverse = require('traverse');
var url = require('url');
var querystring = require('querystring');

var label = require('./label');
var Project = require('./project');
var logger = require('./logger');
var renderPage = require('./render-page');
var recursiveReadSync = require('recursive-readdir-sync')

var defaults = {
    'project-path': '.',
    'project-src-path': path.join('src', 'main', 'webapp'),
    'styleguide-path': 'styleguide',
    'json-suffix': '.json',
    'maven-pom': 'pom.xml',
    'maven-webapp-path': path.join('src', 'main', 'webapp'),
    'template-fallback-extension': '.hbs'
};

module.exports = function (config) {
    config = _.extend({ }, defaults, config);

    logger.welcome();

    let app = express();

    app.use(bodyParser.urlencoded({ extended: true }));

    // API endpoint for Saving
    app.post('/save', function (req, res, next) {
        fs.writeFileSync(req.body.path, req.body.data, 'utf8');

        if (req.body.grunt) {
            exec('npm run grunt', { cwd: config['project-path'] }, function (err) {
                if (err) {
                    res.status(500).send(err);

                } else {
                    res.send('');
                }
            });

        } else {
            res.send('');
        }
    });

    // Automatically generated placeholder images.
    app.use(require('./placeholder-image')());

    // Compile LESS on the fly.
    var cssCachePath = path.join(os.tmpDir(), 'styleguide-css-cache', md5(__dirname));

    app.use(require('less-middleware')(path.join(__dirname, '..', 'styleguide'), { dest: cssCachePath }));
    app.use(express.static(cssCachePath));

    // Custom fonts.
    app.use(require('connect-fonts').setup({
        fonts: [ require("connect-fonts-opensans") ]
    }));

    var project = config.project = new Project(config, config['project-path'])

    project.urlPrefix = '/' + project.name.replace(/ /g, '-').toLowerCase();

    logger.success('Project: ' + project.name);
    logger.success('Project Path: ' + config['project-path']);
    logger.success('Project Source Path: ' + config['project-src-path']);

    // Main display.
    app.use(function (req, res, next) {
        var context = { };

        // Seed for the randomization.
        var seed = context.seed = parseInt(req.query.seed, 10) || Math.floor(Math.random() * 1000000);

        context.project = project;

        // Request URL (e.g. /foo/bar) to file path (e.g. \foo\bar in Windows).
        var requestedUrl = req.path;

        // Is this request pathed under the project prefix?
        if (requestedUrl.indexOf(project.urlPrefix) > -1){
            // strip the project path off
            requestedUrl = req.path.slice(project.urlPrefix.length);
        }

        context.requestedUrl = requestedUrl;

        var requestedPath = context.requestedPath = path.join.apply(path, requestedUrl.split('/'));

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

        var reqSeed = context.reqSeed = req.query.seed;
        // Randomize URL that resets the randomization.
        if (reqSeed) {
            var randomizeUrl = url.parse(req.originalUrl, true);

            delete randomizeUrl.search;
            delete randomizeUrl.query.seed;

            context.randomizeUrl = url.format(randomizeUrl);
        }

        var groups = context.groups = [ ];
        var originalUrlSearch = context.originalUrlSearch = url.parse(req.originalUrl).search || '';

        var reqPath = context.reqPath = req.path;
        var contentQueryString = context.contentQueryString =  querystring.stringify(req.query) || '';

        // Any variable files?
        var varFiles = project.findVariableFiles();

        if (varFiles.length > 0) {
            var varExamples = [ ];
            var varGroup = {
                name: 'Variables',
                children: varExamples
            };

            groups.push(varGroup);

            varFiles.forEach(function (varFile) {
                varExamples.push({
                    name: varFile.name.split('/').map(function (n) { return label(n); }).join(': '),
                    url: project.urlPrefix + '/variables/' + varFile.name
                });
            });
        }
        var examples = context.examples = [ ];
        var groupNavigation = context.groupNavigation =  [ ];
        // Finds all groups of examples in the styleguide directory.

        project.forEachStyleguideItem(function (group, groupPath) {
            var oldDirectory = null;

            function traverseDirectory (dirPath, groupName) {
                var name = path.basename(dirPath);
                var item = {};
                var group = {};

                if (name.slice(0, 1) !== '_') {
                    var urlPath = dirPath.split(project._projectSrcPath).pop();

                    if(path.extname(urlPath) === config['json-suffix']) {
                        // item.name = label(name);
                        item = path.parse(urlPath);
                        item.url = path.join(project.urlPrefix, item.dir, item.name).toLowerCase()+ originalUrlSearch;
                        item.name = label(sentenceCase(item.name));

                        var formatedLabel = (item.dir.charAt(0)==='/')?item.dir.substr(1).replace(/\//g, ' / '): item.dir.replace(/\//g, ' / ')
                          //if formatedLabel is empty set to item.label
                          formatedLabel = formatedLabel ? formatedLabel : item.name
                          item.dirName = label(formatedLabel, /[^a-zA-Z0-9_/]/g);

                        if(oldDirectory !== item.dir){
                            examples.push(group);
                            group.name = item.dirName;
                            groupNavigation = [];
                        }
                        //if the json file is at the root object will not have children.
                        // push item directly to examples
                        if (groupName) {
                          examples.pop()
                          examples.push(item)
                        }else {
                          groupNavigation.push(item)
                          group.children = groupNavigation

                        }


                        oldDirectory = item.dir;
                    }
                    else if (fs.statSync(dirPath).isDirectory()) {
                        try {
                            //if its a directory delete the url so that it doesn't genearate a clickable path.
                            fs.readdirSync(dirPath)
                                .forEach(child => traverseDirectory(path.join(dirPath, child)))
                                .filter(e => !!e);

                        } catch(ex) {
                            if(ex.code == "EACCES")
                                return null;
                        }

                    } else {
                		return null; // Or set item.size = 0 for devices, FIFO and sockets ?
                	}
                return item;
                }
            }
            traverseDirectory(groupPath, group)

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

        // Which stylesheet to display?
        if (config.stylesheets){
            var availableStylesheets = context.availableStylesheets = [ ];
            context.selectedStyleSheet = null;
            config.stylesheets.forEach(function (sheet, i) {
                var key = 'ss';
                var stylesheetUrl = url.parse(req.originalUrl, true);

                // does the original URL contain the query param that matches this sheet?
                if (stylesheetUrl.query[key] == i) {
                    sheet.selected = true;
                    context.selectedStyleSheet = sheet;
                }
                else {
                    sheet.selected = false;
                }

                stylesheetUrl.query[key] = i;
                delete stylesheetUrl.search;
                sheet.idx = i;
                sheet.url = url.format(stylesheetUrl);
                availableStylesheets.push(sheet);
            });
        }


        // Example directory with JSON data files within?
        var examplePath = project.findStyleguideFile(requestedPath);

        // Variables page?
        if (require('./variables')(config, req, res, context)) {
            return;
        }

        // res.send(renderContent(config, context, '/main.hbs'));
        if (require('./render-content')(config, req, res, context)) {
            return;
        }

        // Example JSON data file?
        if (require('./example-file')(config, req, res, context)) {
            return;
        }

        // Not a directory or a styleguide route? (ie: jquery.js)
        if (!examplePath || !fs.statSync(examplePath).isDirectory()) {
            next();
            return;
        }

        // Render the main template.
        logger.success('URL: ' + req.originalUrl);
        return res.send(renderPage(config, context, '/main.hbs'));
    });

    app.use('/node-modules', express.static(path.join(__dirname, '..', 'node_modules')));

    project.forEachPath(function (projectPath) {
        app.use(express.static(projectPath));
        app.use(project.urlPrefix, express.static(projectPath));
    });

    // Load project-specific middleware
    if (config.middlewares) {
      config.middlewares.forEach(function(middleware) {
        app.use(middleware.path, require(middleware.id));
      });
    }

    // Start the web server.
    let server = app.listen(config.port, config.host, function () {
        logger.success(`Started on http://${config.host}:${config.port}`);
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
};
