# Front-end

## Prerequisites

* [Node.js](https://nodejs.org/en/) (>=6.9.1)
* [gulp](https://github.com/gulpjs/gulp/blob/master/docs/getting-started.md)
* [Yarn](https://yarnpkg.com/en/docs/install) (>=0.16.1)

## Installation

Make sure that `gulpfile.js` and `package.json` exist at the root of the project directory.

Download the packages:

    $ yarn

## Usage

Make sure that `styleguide` directory exists at the root of the project directory.

Run Brightspot Styleguide:

    $ gulp styleguide

By default it runs on port `3000`, but you can also run it on a different port:

    $ gulp styleguide --port=3001

Add [example JSON files](docs/example-file-format.rst) to the `styleguide/` directory to see the pages at [http://localhost:3000](http://localhost:3000).

## gulp

* `styleguide.notify(message)`: send a desktop notification.
* `styleguide.serve(settings)`: runs the Styleguide web server with optional settings.
* `styleguide.watch()`: watches JS, JSON, and less files.

### path

* `styleguide.path.build(glob)`: returns the glob in the build directory.
* `styleguide.path.mavenTarget()`: returns the path to the maven snapshot directory. Will return `undefined` when a pom.xml is not found at the root of the project.

### task

* `styleguide.task.copy.templates()`: returns the name of the task thats been pre-configured to copy templates into the build/target directory.
* `styleguide.task.lint.js()`: returns the name of the task that lints JS files.
* `styleguide.task.lint.json()`: returns the name of the task that lints JSON files.
* `styleguide.task.lint.less()`: returns the name of the task that lints less files.
* `styleguide.task.watch()`: returns the name of the task that watches JS, JSON, and less files.

## Troubleshooting

IFrame not resizing properly:
If you change your viewport width and the iframe doesn't resize to the content correctly, this is most likely due to an element height within the iframe, usually the `html` or `body`, set at 100% in the CSS. To avoid this discrepant behavior, make sure the element height is set to auto.
