# Example File Format

If the file's name starts with an underscore, like [_item.json](components/list/_item.json),
it won't show up in the styleguide navigation.

## String Substitution

### Rules

- Number argument can be exact (`3`) or a random number within a range
  (`[ 1, 5 ]`).
- All arguments are optional.
- Parentheses can be omitted when there are no arguments (e.g. `{{words}}`
  instead of `{{words()}}`).

### Methods

#### image

```
{{image(width, height)}}
```

URL to a randomly generated image.

#### name

```
{{name()}}
```

Generate a random name - first and last

#### date

```
{{date()}}

arguments: 'unformatted' | 'short' | 'iso'
```

Generate a random date.
Optional arguments:
 - **defualt** - long date pattern (ie: October 18, 2072 )
 - **'unformatted'** - full date pattern (ie: Tue Oct 18 2072 07:31:28 GMT-0400 (EDT))
 - **'short'** - short date pattern (ie: 10/18/2072)
 - **'iso'** - iso date pattern (ie: 2072-10-18)

#### hexColor

```
{{hexColor(luminosity)}}
```

Randomly generate a hex color with options to choose a luminosity value ranging from dark-light or 0-100 `{{hexColor(25)}`
.To use a different format than hex, you could use the `{{number}}` generator and `hsl` color-space like this:

`"color":"hsl({{number([0,360])}}, 50%, 100%)"`

#### number

```
{{number(number)}}
```

Generate a random number. Option to pass in an array to provide a range of numbers to generate from ie - `{{number([1, 100])}}`.

#### paragraphs

```
{{paragraphs(paragraphCount, sentenceCount, wordCount)}}
```

#### sentences

```
{{sentences(sentenceCount, wordCount)}}
```

#### words

```
{{words(wordCount)}}
```

#### stylesheet

```
{{stylesheet()}}
```

URL to a stylesheet. You define the URL(s) in a `_config.json` at the root of the styleguide directory.
For example:

```json
{
    "stylesheets": [
        {
            "name": "Theme A",
            "href": "/assets/styles/theme-a.min.css"
        },
        {
            "name": "Theme B",
            "href": "/assets/styles/theme-b.min.css"
        }
    ]
}
```

Which will cause the styleguide to render a select list in the upper right. For example:

![image](https://cldup.com/9ACNTLyBkb.png)


## Special Entries

`_dataUrl`

Location of another file that should be merged with the JSON object ([example](components/list/three-items.json)).

`_repeat`

How many times the JSON object should be repeated when it's in a list.
The value can be exact (`3` - [example](components/list/three-items.json))
or a random number within a range (`[ 1, 5 ]` - [example](components/list/many-items.json)).

`_template`

Location of the Handlebars template used to render the JSON object.
The default root path is `src/main/webapp`, so the [link/index.json](components/link/index.json)
`_template` entry of `assets/components/link` points to [src/main/webapp/assets/components/link.hbs](../src/main/webapp/assets/components/link.hbs).
This entry is required unless the object is referenced as either
`displayOptions` ([example](components/list/three-items.json)) or
`extraAttributes` ([example](components/link/index.json)).

`_wrapper`

Location of another file that should wrap the JSON object. That file should
contain an entry of `{ "_delegate": true }` to indicate where the JSON object
should be included ([example](components/link/index.json)).
