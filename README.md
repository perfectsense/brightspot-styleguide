# Installation

Make sure that `package.json` and `Gruntfile.js` exist at the root of the
project directory, and `pom.xml` contains the view generator build plugins.
The examples are in the [example](example) sub-project.

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

Add [example JSON files](example/styleguide) to the `styleguide`
directory to see pages at [http://localhost:3000](http://localhost:3000).
