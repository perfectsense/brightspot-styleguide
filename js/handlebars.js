const _ = require('lodash')
const escapeHtmlInJson = require('escape-html-in-json')
const fs = require('fs')
const handlebars = require('handlebars')
const path = require('path')

const DataGenerator = require('./data-generator')
const resolver = require('./resolver')

// Set up Handlebars cache.
const compiledTemplates = { }

function compile (path) {
  if (!compiledTemplates[path]) {
    compiledTemplates[path] = {
      time: fs.statSync(path).mtime.getTime(),
      template: handlebars.compile(fs.readFileSync(path, 'utf8'))
    }
  } else {
    const newTime = fs.statSync(path).mtime.getTime()

    if (compiledTemplates[path].time !== newTime) {
      compiledTemplates[path] = {
        time: newTime,
        template: handlebars.compile(fs.readFileSync(path, 'utf8'))
      }
    }
  }

  return compiledTemplates[path].template
}

// Returns all pairs who's keyname doesn't start with an '_'
function removePrivateKeys (data) {
  return _.omit(data, function (value, key) {
    return _.startsWith(key, '_')
  })
}

function renderJson (data) {
  return compile(path.join(__dirname, 'example-json.hbs'))({
    'json': JSON.stringify(removePrivateKeys(data), escapeHtmlInJson, 2)
  })
}

function render (data) {
  const templatePath = data._template
  return !templatePath && data._view ? renderJson(data) : compile(templatePath)(data)
}

function Template () {
}

Template.prototype.toHTML = function () {
  if (!this._html) {
    this._html = render(this)
  }

  return this._html
}

module.exports = function (styleguide) {
  handlebars.registerHelper('render', function (data, fullScope) {
    if (!data) {
      return ''
    }

    if (typeof data !== 'object') {
      return data.toString()
    }

    data = _.extend({ }, data, fullScope.hash)

    return new handlebars.SafeString(render(data))
  })

  // Marks the path to be uploaded to the CDN.
  handlebars.registerHelper('cdn', function (path) {
    return path
  })

  // This helper returns the key/value pairs of extraAttributes key , separated with '=' as a string. Used in links, images, forms.
  handlebars.registerHelper('extraAttributes', function (context) {
    if (!context) return

    const data = context.data
    if (!data) return

    const root = data.root
    if (!root) return

    const attrs = root.extraAttributes
    if (!attrs) return

    return new handlebars.SafeString(Object
      .keys(attrs)
      .map(name =>
        ' ' +
        handlebars.Utils.escapeExpression(name) +
        '="' + handlebars.Utils.escapeExpression(attrs[name]) +
        '"')
      .join(''))
  })

  // This helper returns the key/value pairs of jsonObject key as an object string. Used when we need to pass a JSON object as a string into some JS options
  handlebars.registerHelper('jsonObject', function (context, options) {
    let jsonObjectData

    if (!context) {
      return
    }

    try {
      jsonObjectData = context.data.root.jsonObject
    } catch (e) {
      jsonObjectData = context
    }

    return new handlebars.SafeString(JSON.stringify(jsonObjectData))
  })

  // BEM helpers:
  const PREFIX = '_bem_'
  const BLOCK_NAME = PREFIX + 'blockName'
  const PARENT_PATH = PREFIX + 'parentPath'
  const NO_OVERRIDE = PREFIX + 'noOverride'
  const BLOCK_BODY_OPTIONS = PREFIX + 'blockBodyOptions'
  const ELEMENT_NAME = PREFIX + 'elementName'
  const ELEMENT_OPTIONS_PREFIX = PREFIX + 'elementOptions_'

  function bemHelper (helper) {
    return function () {
      const oldKeys = { }
      const oldValues = { }
      const options = arguments.length > 1 ? arguments[1] : arguments[0]

      Array.prototype.unshift.call(arguments, {
        isTrue: value => !!value,
        isBlock: () => !!options.fn,
        safe: value => new handlebars.SafeString(value),
        get: key => options.data.root[key],

        setRoot: (key, value) => {
          options.data.root[key] = value
          return value
        },

        set: (key, value) => {
          if (!oldKeys[key]) {
            oldKeys[key] = true
            oldValues[key] = options.data.root[key]
          }
          options.data.root[key] = value
          return value
        },

        resolve: function (path) {
          return resolver.path(styleguide.path.root(), this.get(PARENT_PATH) || this.get('_template'), path)
        }
      })

      const result = helper.apply(this, arguments)

      Object.keys(oldKeys).forEach(key => {
        options.data.root[key] = oldValues[key]
      })

      return result
    }
  }

  // Defines or renders a block.
  const block = bemHelper(function (bem, name, options) {
    if (!bem.isTrue(bem.get(BLOCK_NAME))) {
      bem.set(BLOCK_NAME, name)
    }

    const override = options.hash.override
    const extend = override || options.hash.extend

    if (bem.isTrue(extend)) {
      const extendPath = bem.set(PARENT_PATH, bem.resolve(extend))
      const extendTemplate = compile(extendPath)
      const extendResult = extendTemplate(this, options)

      if (!bem.isBlock()) {
        return bem.safe(extendResult)
      }

      if (bem.isTrue(override)) {
        options.fn(this)
        bem.set(NO_OVERRIDE, true)
        return bem.safe(extendTemplate(this, options))
      }
    }

    if (bem.isBlock()) {
      return bem.safe(options.fn(this))
    } else {
      throw new Error(`{{block}} without extend in [${bem.get('_template')}] must have a body!`)
    }
  })

  handlebars.registerHelper('block', block)

  // Returns the name of the current block.
  handlebars.registerHelper('blockName', bemHelper(function (bem, options) {
    return bem.get(BLOCK_NAME)
  }))

  // Defines or renders the body of the current block.
  const blockBody = bemHelper(function (bem, options) {
    if (bem.isBlock() && !bem.isTrue(bem.get(NO_OVERRIDE))) {
      bem.setRoot(BLOCK_BODY_OPTIONS, options)
      return bem.safe(options.fn(this))
    }

    const blockBodyOptions = bem.get(BLOCK_BODY_OPTIONS)

    if (bem.isTrue(blockBodyOptions)) {
      return bem.safe(blockBodyOptions.fn(this))
    } else {
      return null
    }
  })

  handlebars.registerHelper('blockBody', blockBody)

  // Defines or renders an element.
  handlebars.registerHelper('element', bemHelper(function (bem, name, options) {
    if (bem.isBlock() && !bem.isTrue(bem.get(NO_OVERRIDE))) {
      bem.setRoot(ELEMENT_OPTIONS_PREFIX + name, options)
    }

    const elementOptions = bem.get(ELEMENT_OPTIONS_PREFIX + name)

    if (!bem.isTrue(elementOptions)) {
      throw new Error(`[${name}] element not defined in [${bem.get('_template')}]!`)
    }

    bem.set(ELEMENT_NAME, bem.get(BLOCK_NAME) + '-' + name)

    const value = this[name]

    if (bem.isTrue(elementOptions.hash.noWith)) {
      return bem.safe(elementOptions.fn(this))
    } else if (bem.isTrue(value)) {
      return bem.safe(elementOptions.fn(value))
    } else {
      return bem.safe(elementOptions.inverse(value))
    }
  }))

  // Returns the name of the current element.
  handlebars.registerHelper('elementName', bemHelper(function (bem, options) {
    return bem.get(ELEMENT_NAME)
  }))

  // Deprecated BEM helpers:
  handlebars.registerHelper('defineBlock', block)

  handlebars.registerHelper('defineBlockContainer', function (options) {
    return options.fn(this)
  })

  handlebars.registerHelper('defineBlockBody', blockBody)
  handlebars.registerHelper('defaultBlockBody', blockBody)

  handlebars.registerHelper('defineElement', bemHelper(function (bem, name, options) {
    if (bem.isBlock() && !bem.isTrue(bem.get(NO_OVERRIDE))) {
      bem.setRoot(ELEMENT_OPTIONS_PREFIX + name, options)
    }
  }))

  // Render the example file data.
  const exampleTemplate = compile(path.join(__dirname, 'example.hbs'))

  function convert (data, inArray) {
    if (!data) return
    if (typeof data !== 'object') return data

    let array

    if (Array.isArray(data)) {
      array = data.map(item => convert(item, true))
    } else {
      const template = data._template || data._view ? new Template() : { }

      Object.keys(data).forEach(key => {
        template[key] = convert(data[key])
      })

      if (inArray) {
        return template
      } else {
        array = [ template ]
      }
    }

    array.toHTML = function () {
      return this.reduce((acc, item) => acc + (item && item.toHTML ? item.toHTML() : item.toString()), '')
    }

    return array
  }

  return function (data) {
    return exampleTemplate({ data: convert(new DataGenerator(styleguide, styleguide.randomSeed()).process(data)) })
  }
}
