const fs = require('fs')
const handlebars = require('handlebars')
const _ = require('lodash')
const escapeHtml = require('escape-html-in-json')
const path = require('path')
const traverse = require('traverse')

const DataGenerator = require('./data-generator')
const resolver = require('./resolver')

module.exports = function (styleguide, filePath) {
  const buildPath = styleguide.path.build()
  let data = resolver.data(buildPath, filePath)

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
    const wrap = (data, wrapperPath) => {
      if (!wrapperPath) {
        wrapperPath = data._wrapper

        if (!wrapperPath) {
          return data
        }
      }

      // Make sure that the wrapper exists.
      const resolvedWrapperPath = resolver.path(buildPath, filePath, wrapperPath)

      if (!fs.existsSync(resolvedWrapperPath)) {
        throw new Error(`Wrapper at [${resolvedWrapperPath}] doesn't exist!`)
      }

      // Put the existing data into the wrapper at _delegate marker object.
      const wrapper = resolver.data(buildPath, resolvedWrapperPath)

      traverse(wrapper).forEach(function (value) {
        if (value && value._delegate) {
          this.update(data)
        }
      })

      return wrap(wrapper)
    }

    // Wrapper specified explicitly?
    let wrapperPath = data._wrapper

    if (wrapperPath) {
      data = wrap(data)
    } else {
      // Use the wrapper at root implicitly.
      wrapperPath = resolver.path(buildPath, filePath, '/styleguide/_wrapper.json')

      if (fs.existsSync(wrapperPath)) {
        data = wrap(data, '/styleguide/_wrapper.json')
      }
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
      return _.startsWith(key, '_')
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

  // Defines a block.
  var DATA_PREFIX = '_brightspot_'
  var ELEMENT_DATA = DATA_PREFIX + 'element'
  var BLOCK_NAME_DATA = DATA_PREFIX + 'blockName'
  var DEFINE_BLOCK_CONTAINER_IN_EXTEND_DATA = DATA_PREFIX + 'defineBlockContainerInExtend'

  function compile (absPath, options) {
    return handlebars.compile(fs.readFileSync(absPath, 'utf8'))
  }

  handlebars.registerHelper('defineBlock', function (name, options) {
    if (!options.data[BLOCK_NAME_DATA]) {
      options.data[BLOCK_NAME_DATA] = name
    }

    let extend = options.hash.extend

    // block helper extend parameter was set
    if (extend) {
      let absolutePath = resolver.path(styleguide.path.root(), options.data.root._contextPath || options.data.root._template, extend)
      options.data.root._contextPath = absolutePath
      var template = compile(absolutePath, options)
      var templateOptions = { data: { } }
      templateOptions.data[BLOCK_NAME_DATA] = name
      templateOptions.data[DEFINE_BLOCK_CONTAINER_IN_EXTEND_DATA] = true

      template(this, templateOptions)
    }

    return new handlebars.SafeString(options.fn(this))
  })

  // Marks the template as the block container.
  handlebars.registerHelper('defineBlockContainer', function (options) {
    if (options.data[DEFINE_BLOCK_CONTAINER_IN_EXTEND_DATA]) {
      return null
    } else {
      return new handlebars.SafeString(options.fn(this))
    }
  })

  // Marks the template as the block body.
  var BLOCK_BODY_TEMPLATE_DATA = DATA_PREFIX + 'blockBodyTemplate'

  function defineBlockBody (options) {
    var overrideTemplate = this[BLOCK_BODY_TEMPLATE_DATA]
    var override

    if (overrideTemplate) {
      override = overrideTemplate(this, options)
    } else {
      override = options.fn(this)
    }

    return new handlebars.SafeString(override)
  }

  handlebars.registerHelper('defineBlockBody', defineBlockBody)
  handlebars.registerHelper('defaultBlockBody', defineBlockBody)

  // Returns the name of the current block.
  handlebars.registerHelper('blockName', function (options) {
    return options.data[BLOCK_NAME_DATA]
  })

  // Renders a block, optionally with a new name and replacing its body.
  handlebars.registerHelper('block', function (extend, options) {
    var template = compile(resolver.path(styleguide.path.root(), options.data.root._contextPath || options.data.root._template, extend))
    this[BLOCK_BODY_TEMPLATE_DATA] = options.fn
    var templateOptions = { data: { } }
    templateOptions.data[BLOCK_NAME_DATA] = options.hash.name

    return new handlebars.SafeString(template(this, templateOptions))
  })

  // Defines an element.
  var ELEMENT_NAME_DATA = DATA_PREFIX + 'elementName'
  var ELEMENT_DEFINITION_DATA_PREFIX = DATA_PREFIX + 'element_'

  handlebars.registerHelper('defineElement', function (name, options) {
    this[ELEMENT_DEFINITION_DATA_PREFIX + name] = options
  })

  // Returns the name of the current element.
  handlebars.registerHelper('elementName', function (options) {
    return options.data[ELEMENT_NAME_DATA]
  })

  // Renders the named element.
  handlebars.registerHelper('element', function (name, options) {
    var self = this
    var elementOptions = this[ELEMENT_DEFINITION_DATA_PREFIX + name]

    if (!elementOptions) {
      throw new Error('[' + name + '] element not defined!')
    }

    // does this element contain nested elements?
    Object.keys(this).filter(function (key) {
      if (key.indexOf(ELEMENT_DATA) > -1 && options.data[BLOCK_NAME_DATA]) {
        self[key].data[BLOCK_NAME_DATA] = options.data[BLOCK_NAME_DATA]
      }
    })

    var value = this[name]
    var fnOptions = { data: { } }
    var blockName = options.data[BLOCK_NAME_DATA] || elementOptions.data[BLOCK_NAME_DATA]

    fnOptions.data[ELEMENT_NAME_DATA] = blockName + '-' + name

    if (elementOptions.hash.noWith) {
      value = elementOptions.fn(this, fnOptions)
    } else if (value) {
      value = elementOptions.fn(value, fnOptions)
    } else {
      value = elementOptions.inverse(value, fnOptions)
    }

    return new handlebars.SafeString(value)
  })

  return template({ data: convert(data) }).replace('</body>', '<script type="text/javascript">window.parent.rebase(window)</script></body>')
}
