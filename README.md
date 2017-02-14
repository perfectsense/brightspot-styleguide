# Brightspot Styleguide

All the rules and guidelines in https://github.com/perfectsense/brightspot
apply to this repository as well. This is separated out only because npm
doesn't allow linking to a subdirectory (https://github.com/npm/npm/issues/2974).

# Front-end

## Prerequisites

* [Node.js](https://nodejs.org/en/) (>=6.9.1)
> We recommend using [NVM](https://github.com/creationix/nvm#install-script) to install and manage Node.js
* [gulp](https://github.com/gulpjs/gulp/blob/master/docs/getting-started.md) (3.9.x)
> $ npm install gulp --global
* [Yarn](https://yarnpkg.com/en/docs/install) (>=0.16.1)
> $ curl -o- -L https://yarnpkg.com/install.sh | bash

## Installation

> **Important!**: Run `$ node -v` or `$ nvm ls` to be sure that your shell is using the version of Node.js listed in the Prerequisites above.


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

## Handlebars Helpers

* `block`
* `blockName`
* `blockBody`
* `element`
* `elementName`

### Handlebars Usage Examples

Given:

```hbs
{{#block "Promo"}}
    <div class="{{blockName}}">
        {{#blockBody}}
            {{#element "title"}}<div class="{{elementName}}">{{this}}</div>{{/element}}
            {{#element "description"}}<div class="{{elementName}}">{{this}}</div>{{/element}}
            {{#element "figure" noWith=true}}
                <figure class="{{elementName}}">
                    {{#element "image"}}{{this}}{{/element}}
                    {{#element "caption"}}<figcaption class="{{elementName}}">{{this}}</figcaption>{{/element}}
                </figure>
            {{/element}}
        {{/blockBody}}
    </div>
{{/block}}
```

How do I rename it?

```hbs
{{block "MyPromo" override="Promo.hbs"}}
```

How do I reorder or remove elements?

```hbs
{{#block "MyPromo" override="Promo.hbs"}}
    {{#blockBody}}
        {{element "figure"}}
        {{element "title"}}
    {{/blockBody}}
{{/block}}
```

How do I change the `caption` element markup?

```hbs
{{#block "MyPromo" override="Promo.hbs"}}
    {{#element "caption"}}
        <div class="{{elementName}}">{{this}}</div>
    {{/element}}
{{/block}}
```

How do I change the outer block markup?

```hbs
{{#block "MyPromo" extend="Promo.hbs"}}
    <section class="{{blockName}}">{{blockBody}}</section>
{{/block}}
```

## Styling Components

The default style of a component should be _mobile first_.

### Styling Usage Examples

Where should I define media queries?

> Within a `MediaQueries.less` file imported from `All.less`

Given: `Promo.less`:

```less
.Promo {
  &-title {
    color: dodgerblue;
  }
}
```

How do I define a small and large size?

```less
.Promo--small() {
  &-title {
    font-size: 10px;
  }
}

.Promo--large() {
  &-title {
    font-size: 20px;
  }
}
```

How do I default it to the small size?

```less
.Promo {
  &-title {
    color: dodgerblue;
  }

  .Promo--small;
}
```

How do I change to the large size at a specific breakpoint?

> Use media queries within your `MediaQueries.less` file:

```less
@media "only screen and (min-width: 768px)" {
  .Promo {
    .Promo--large;
  }
}
```

How do I change to the large size _only_ when it's a child of the right-rail?

> Use container queries within your `RightRail.less` file:

```less
.RightRail {
  .Promo {
    .Promo--large;
  }
}
```

## Troubleshooting

IFrame not resizing properly:
If you change your viewport width and the iframe doesn't resize to the content correctly, this is most likely due to an element height within the iframe, usually the `html` or `body`, set at 100% in the CSS. To avoid this discrepant behavior, make sure the element height is set to auto.
