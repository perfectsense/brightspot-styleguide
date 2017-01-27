# Styling

Our styles use the [LESS](http://lesscss.org/) CSS pre-processor. We enforce our coding standards via [lesshint](https://github.com/lesshint/lesshint); which is pre-configured in the build process to fail the build when one or more of our conventions aren't followed.

> **Important!**
Be sure that your editor/IDE of choice supports [editorconfig](http://editorconfig.org/) to preserve formatting between developer environments. To see what our formatting standards are, take a look at your project's `.editorconfig` file.

## Recommendations

We recommend using the following plugins:

#### Atom:
+ [linter-lesshint](https://atom.io/packages/linter-lesshint)

Examples
---


Where do I begin introducing styles to my project?

> At the root of the project's styleguide, create an `All.less` file. This file serves as a place to define global variables and the root of all import statements.

Given All.less:

```less
@import 'promo/Promo'
```

> As the project matures, you'll import more than one file from the same directory path ...

```less
@import 'promo/Promo'
@import 'promo/MegaPromo'
```

... which you'll change by creating an `All.less` within the _promo_ directory.

Given `promo/All.less`:

```less
@import 'Promo'
@import 'MegaPromo'
```

And now, `All.less` looks like:

```less
@import 'promo/All'
```

How do I start writing styles for a _Promo_ component?

> Within the same directory as `Promo.hbs`, create a `Promo.less` file.

```less
.Promo {

}
```

Where do I define its default styles?

> Define default/mobile-first styles in the opening selector block

```less
.Promo {
  padding: 1rem;

  &-title {
    text-transform: uppercase;
  }
}
```

How do I define its style variations; like a _large promo_ or a _bordered promo_?

> Define variations as mix-in functions below the opening selector block. Mix-in names start with the same name as the component, followed by a double-dash, then the variation name is camelCased and we suffix it all with `()`.

```less
.Promo {
  padding: 1rem;

  &-title {
    text-transform: uppercase;
  }
}

.Promo--bordered() {
  border: dashed 1px dodgerblue;
}

.Promo--large() {
  padding: 2rem;

  &-title {
    font-size: 30px;
  }
}
```

How do I re-use these styles in a _MegaPromo_ component?

> Use LESS `extend`

Given MegaPromo.less:

```less
@import 'Promo';

.MegaPromo {
  &:extend(.Promo all);

  &-title {
    color: blue;
  }
}
```

How do I apply a specific style variation when its in the _FullWidth-body_ layout?

> Use container-queries defined in a layout file

Given layout/FullWidth.less:

```less
.FullWidth-body {
  > .Promo {
    .Promo--large;
  }
}
```

How do I apply a specific style variation at a specific breakpoint?

> Use container-queries defined in a layout file

Given layout/TwoColumn.less:

```less
@media "only screen and (min-width: 768px)" {
  .TwoColumn-left {
    > .Promo {
      .Promo--large;
    }
  }

  .TwoColumn-right {
    > .Promo {
      .Promo--bordered;
    }
  }
}
```

FAQ
---

_Why do you suffix mix-ins with `()`?_

Adding the parenthesis ensures that mix-in won't be output on its own. See [the LESS docs](http://lesscss.org/features/#mixins-feature-not-outputting-the-mixin).


_Do I need to add vendor prefixes to my styles?_

Nope. The build will take care of that using [auto-prefixer](https://github.com/postcss/autoprefixer). This is part of the build process in your project's `gulpfile.js`

_Besides the root `All.less`, when should I add an `All.less` file to a sub-directory?_

When your sub-directory contains more than one LESS file, its time to create an `All.less`.
