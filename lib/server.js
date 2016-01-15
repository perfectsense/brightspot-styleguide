var bodyParser = require('body-parser');
var exec = require('child_process').exec;
var express = require('express');
var fs = require('fs');
var _ = require('lodash');
var marked = require('marked');
var md5 = require('md5');
var os = require('os');
var path = require('path');
var traverse = require('traverse');
var url = require('url');
var querystring = require('querystring');

var label = require('./label');
var Library = require('./library');
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
    'maven-webapp-path': path.join('src', 'main', 'webapp')
};

module.exports = function (config) {
    logger.welcome();

    var app = express();

    // Save API endpoint.
    app.use(bodyParser.urlencoded({ extended: true }));

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

    config = _.extend({ }, defaults, config);

    logger.success('Project Path: ' + config['project-path']);

    // Find all libraries.
    var libraries = config.libraries = [ ];

    function addLibrary(basePath) {
        var library = new Library(config, basePath);

        if (library.isLibrary()) {
            library.urlPrefix = '/' + library.name.replace(/ /g, '-').toLowerCase();

            libraries.push(library);
        }

        return library;
    }

    function addChildLibraries(parentPath) {
        if (fs.existsSync(parentPath)) {
            fs.readdirSync(parentPath).forEach(function (child) {
                addLibrary(path.join(parentPath, child));
            });
        }
    }

    var projectLibrary = config.projectLibrary = addLibrary(config['project-path']);

    // merge any project specific library configs overtop the current config
    config = projectLibrary.mergeConfig(config);

    addChildLibraries(path.join(config['project-path'], 'bower_components'));

    libraries.forEach(function (library) {
        logger.success('Library: ' + library.name);
    });

    // Main display.
    app.use(function (req, res, next) {
        var context = { };

        // Seed for the randomization.
        var seed = context.seed = parseInt(req.query.seed, 10) || Math.floor(Math.random() * 1000000);

        // Selected library?
        var availableLibraries = context.availableLibraries = [ ];
        var library;

        config.libraries.forEach(function (l) {
            var al = { library: l };

            availableLibraries.push(al);

            if (req.url.indexOf(l.urlPrefix) === 0) {
                library = l;
                al.selected = true;
            }
        });

        if (!library) {
            library = libraries[0];
        }

        context.library = library;

        // Request URL (e.g. /foo/bar) to file path (e.g. \foo\bar in Windows).
        var requestedUrl = context.requestedUrl = req.path.slice(library.urlPrefix.length);
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

        // Randomize URL that resets the randomization.
        if (req.query.seed) {
            var randomizeUrl = url.parse(req.originalUrl, true);

            delete randomizeUrl.search;
            delete randomizeUrl.query.seed;

            context.randomizeUrl = url.format(randomizeUrl);
        }

        var groups = context.groups = [ ];
        var originalUrlSearch = url.parse(req.originalUrl).search || '';

        // Any variable files?
        var varFiles = library.findVariableFiles();

        if (varFiles.length > 0) {
            var varExamples = [ ];
            var varGroup = {
                name: 'Variables',
                examples: varExamples
            };

            groups.push(varGroup);

            varFiles.forEach(function (varFile) {
                varExamples.push({
                    name: varFile.name.split('/').map(function (n) { return label(n); }).join(': '),
                    url: library.urlPrefix + '/variables/' + varFile.name
                });
            });
        }

        // Finds all groups of examples in the styleguide directory.
        library.forEachStyleguideGroup(function (group, groupPath) {
            var examples = [ ];

            // Finds all examples in this group.
            fs.readdirSync(groupPath).forEach(function (example) {
                if (example.slice(0, 1) !== '_') {
                    var examplePath = path.join(groupPath, example);

                    if (fs.statSync(examplePath).isDirectory()) {
                        examples.push({
                            name: label(example),
                            url: library.urlPrefix + '/' + group + '/' + example + originalUrlSearch
                        });
                    }
                }
            });

            if (examples.length > 0) {
                groups.push({
                    name: 'Examples: ' + label(group),
                    examples: examples
                });
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

        // Variables page?
        if (require('./variables')(config, req, res, context)) {
            return;
        }

        // Example JSON data file?
        if (require('./example-file')(config, req, res, context)) {
            return;
        }

        // Example directory with JSON data files within?
        var examplePath = library.findStyleguideFile(requestedPath);

        if (!examplePath || !fs.statSync(examplePath).isDirectory()) {
            next();
            return;
        }

        // Finds all example files mapped to the request URL.
        var files = context.files = [ ];
        var mainFile;

        fs.readdirSync(examplePath).forEach(function (file) {
            if (file.slice(0, 1) !== '_' && path.extname(file) === config['json-suffix']) {
                var options;
                var exampleFilePath = path.join(examplePath, file);
                var exampleFileData = fs.readFileSync(exampleFilePath, 'utf8');

                traverse(JSON.parse(exampleFileData)).forEach(function (value) {
                    if (this.key === 'displayOptions' &&
                            _.isPlainObject(value)) {

                        var prefix = this.path.join('/') + '/';

                        Object.keys(value).forEach(function (key) {
                            if (!options) {
                                options = { };
                            }

                            options[prefix + key] = value[key];
                        });
                    }
                });

                var name = path.basename(file, config['json-suffix']);
                var url = req.path + '/' + name;
                var f = {
                    name: label(name),
                    url: url,
                    jsonUrl: url + '.json',
                    exampleFilePath: exampleFilePath,
                    exampleFileData: exampleFileData,
                    options: options
                };

                var descriptionPath = exampleFilePath.slice(0, -config['json-suffix'].length) + '.md';

                if (fs.existsSync(descriptionPath)) {
                    f.description = marked(fs.readFileSync(descriptionPath, 'utf8'));
                }

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
        res.send(renderPage(config, context, '/main.hbs'));
        return;
    });

    app.use('/node-modules', express.static(path.join(__dirname, '..', 'node_modules')));

    projectLibrary.forEachPath(function (libraryPath) {
        app.use(express.static(libraryPath));
    });

    libraries.forEach(function (library) {
        library.forEachPath(function (libraryPath) {
            app.use(library.urlPrefix, express.static(libraryPath));
        });
    });

    // Start the web server.
    app.listen(config.port, config.host, function () {
        logger.success('Started on http://' + config.host + ':' + config.port);
    });
};
