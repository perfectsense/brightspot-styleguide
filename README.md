# Front-end

## Prerequisites

* [Node.js](https://nodejs.org/en/)
* [Yarn](https://yarnpkg.com/en/docs/install)

## Installation

Make sure that `gulpfile.js` and `package.json` exist at the root of the project directory.

Download the packages:

    $ yarn

## Usage

Make sure that `styleguide` directory exists at the root of the project directory.

Run Brightspot Styleguide:

    $ gulp styleguide

You can also run it on a different port:

    $ gulp styleguide --port=3001

Add [example JSON files](example/styleguide) to the `styleguide` directory to see the pages at [http://localhost:3000](http://localhost:3000).

## Troubleshooting

IFrame not resizing properly:
If you change your viewport width and the iframe doesn't resize to the content correctly, this is most likely due to an element height within the iframe, usually the `html` or `body`, set at 100% in the CSS. To avoid this discrepant behavior, make sure the element height is set to auto.
