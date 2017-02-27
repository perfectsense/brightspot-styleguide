const _ = require('lodash')
const escapeHtml = require('escape-html-in-json')
const findParentDir = require('find-parent-dir')
const fs = require('fs')
const handlebars = require('handlebars')
const path = require('path')
const traverse = require('traverse')

const DataGenerator = require('./data-generator')
const resolver = require('./resolver')

module.exports = function (styleguide, filePath) {
  const buildPath = styleguide.path.build()
  let data = resolver.data(buildPath, filePath)

  if (Object.keys(data).length === 0) {
    throw new Error(`Example data contains zero keys`)
  }

  if (data._hidden) {
    return
  }

  // Validate the JSON data. Exceptions for the special keys we have that are maps, so they don't need _template or _view
  traverse(data).forEach(function (value) {
    if (_.isPlainObject(value) &&
      !value._template &&
      !value._view &&
      this.key.slice(0, 1) !== '_' &&
      this.key !== 'displayOptions' &&
      this.key !== 'extraAttributes' &&
      this.key !== 'jsonObject') {
      var safeParent = false

      this.parents.forEach(function (value) {
        if (this.key !== 'jsonObject') {
          safeParent = true
        }
      })

      if (!safeParent) {
        throw new Error('Object without _template or _view entry at ' + this.path.join('/') + '!')
      }
    }
  })

  // Wrap the example file data?
  if (data._wrapper !== false && !data._view) {
    // Wrapper specified explicitly or use the closest?
    let wrapperPath = data._wrapper

    if (wrapperPath) {
      wrapperPath = resolver.path(buildPath, filePath, wrapperPath)
    } else {
      wrapperPath = findParentDir.sync(filePath, '_wrapper.json')

      if (wrapperPath) {
        wrapperPath = path.join(wrapperPath, '_wrapper.json')
      }
    }

    if (wrapperPath) {
      if (!fs.existsSync(wrapperPath)) {
        throw new Error(`Wrapper at [${wrapperPath}] doesn't exist!`)
      }

      // Put the existing data into the wrapper at _delegate marker object.
      const wrapper = resolver.data(buildPath, wrapperPath)

      traverse(wrapper).forEach(function (value) {
        if (value && value._delegate) {
          this.update(data)
        }
      })

      data = wrapper
    }
  }

  // post-process the JSON data.
  new DataGenerator(styleguide).process(data)

  // Set up Handlebars cache.
  var compiledTemplates = { }

  function renderTemplate (data) {
    var templatePath = data._template

    // See if this is a JSON view and render the data as JSON using a simple HTML template
    if (!templatePath && data._view) {
      return renderJsonTemplate(data)
    }

    var compiledTemplate = compiledTemplates[templatePath]

    if (!compiledTemplate) {
      var template = fs.readFileSync(templatePath, 'utf8')

      compiledTemplate = handlebars.compile(template)
      compiledTemplates[templatePath] = compiledTemplate
    }

    return compiledTemplate(data)
  }

  function renderJsonTemplate (data) {
    var jsonData = {
      'json': JSON.stringify(removePrivateKeys(data), escapeHtml, 2)
    }
    var jsonTemplate = fs.readFileSync(path.join(__dirname, 'example-json.hbs'), 'utf8')
    var jsonCompiledTemplate = handlebars.compile(jsonTemplate)

    return jsonCompiledTemplate(jsonData)
  }

  // Returns all pairs who's keyname doesn't start with an '_'
  function removePrivateKeys (data) {
    return _.omit(data, function (value, key) {
      if (Object(value) === value) {
        removePrivateKeys(value)
      }

      return _.startsWith(key, '_')
        && !_.eq(key, '_view')
    })
  }

  handlebars.registerHelper('render', function (data, fullScope) {
    if (!data) {
      return ''
    }

    if (typeof data !== 'object') {
      return data.toString()
    }

    data = _.extend({ }, data, fullScope.hash)

    return new handlebars.SafeString(renderTemplate(data))
  })

  // Render the example file data.
  var template = handlebars.compile(fs.readFileSync(path.join(__dirname, 'example.hbs'), 'utf8'))

  function Template () {
  }

  Template.prototype.toHTML = function () {
    return renderTemplate(this)
  }

  function convert (data) {
    if (!data) return

    if (typeof data === 'object') {
      if (Array.isArray(data)) {
        return data.map(function (item) {
          return convert(item)
        })
      } else {
        var copy = {}
        if (data._template || data._view) {
          copy = new Template()
        }

        Object.keys(data).forEach(function (key) {
          copy[key] = convert(data[key])
        })

        return copy
      }
    }

    return data
  }

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
    var jsonObjectData

    if (!context) {
      return
    }

    if (!context.data.root.jsonObject) {
      return
    }

    jsonObjectData = context.data.root.jsonObject

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
      const extendTemplate = handlebars.compile(fs.readFileSync(extendPath, 'utf8'))
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

  return template({ data: convert(data) })
}
