# Installation

Make sure that `bower.json`, `Gruntfile.js`, and `package.json` exist at the
root of the project directory, and `pom.xml` contains the view generator build
plugins. The examples are in the [example](example) sub-project.

Build the project:

    (FE Developer)$ npm install && npm run grunt
    (Java Developer)$ mvn clean package

# Usage

Make sure that `styleguide` directory exists at the root of the project
directory.

Run Brightspot Styleguide:

    (FE Developer)$ npm run styleguide
    (Java Developer)$ target/bin/styleguide

(Optional) Synchronize CSS/JS changes:

    (FE Developer)$ npm run grunt -- watch
    (Java Developer)$ target/bin/grunt watch

If you need to run the styleguide on a different port or directly on a host, you can pass those options into the command line. For example, to utilize the styleguide on a Virtual Machine, you need to run it on your local IP vs just `locahost`

    $ npm run styleguide -- --port=3001 --host=192.168.0.100

Add [example JSON files](example/styleguide) to the `styleguide`
directory to see pages at [http://localhost:3000](http://localhost:3000).

# Troubleshooting

IFrame not resizing properly:
If you change your viewport width and the iframe doesn't resize to the content correctly, this is most likely due to an element height within the iframe, usually the `html` or `body`, set at 100% in the CSS. To avoid this discrepant behavior, make sure the element height is set to auto.
