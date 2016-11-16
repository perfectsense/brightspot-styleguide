const fs = require('fs')
const handlebars = require('handlebars')
const _ = require('lodash')
const Util = require('./util')
const escapeHtml = require('escape-html-in-json')
const path = require('path')
const traverse = require('traverse')
const DataGenerator = require('./data-generator')

function resolvePath (basePath, filePath, reqPath = null, parentPath = null) {
  let resolvedPath = ''
  if (filePath.split(path.sep)[1] === 'node_modules') {
    resolvedPath = basePath
  } else if (path.isAbsolute(filePath) && fs.existsSync(filePath)) {
    resolvedPath = filePath
    return resolvedPath
  } else if (path.isAbsolute(filePath)) {
    let context = parentPath || path.join(basePath, path.dirname(filePath))
    resolvedPath = Util.closestParentWithFile(context, 'package.json')
  } else if (parentPath) {
    if (fs.lstatSync(parentPath).isFile()) {
      resolvedPath = path.dirname(parentPath)
    } else {
      resolvedPath = parentPath
    }
  } else {
    resolvedPath = path.dirname(reqPath)
  }

  resolvedPath = path.join(resolvedPath, filePath)

  if (!fs.existsSync(resolvedPath)) {
    throw new Error(`Couldn't find file: ${resolvedPath}`)
  }

  return resolvedPath
}

function resolveTemplatePath (data, requestPath, project) {
  traverse(data).forEach(function (value) {
    if (!value) return

    let templatePath = value._template
    let parentPath = (this.parent) ? this.parent.node._dataUrl : null

    if (templatePath) {
      value._template = resolvePath(project._projectPath, templatePath, requestPath, parentPath)
    }
  })
}

function resolveDataUrlPaths (data, currentDataPath, project) {
  traverse(data).forEach(function (value) {
    if (!value) return

    let parentPath = (this.parent) ? this.parent.node._dataUrl : null
    let dataUrl = value._dataUrl
    let dataPath = ''

    if (dataUrl) {
      dataPath = resolvePath(project._projectPath, dataUrl, currentDataPath, parentPath)

      let originalDataPath = currentDataPath
      currentDataPath = dataPath

      // parse the JSON file
      try {
        var data = JSON.parse(fs.readFileSync(dataPath, 'utf8'))
      } catch (err) {
        throw err
      }

      // update the dataUrl
      this.node._dataUrl = path.dirname(dataPath)

      // store and update the template path relative to the parent template that _dataUrl'd it
      data._template = path.resolve(path.dirname(dataPath), data._template)

      // extend the incoming data from `dataUrl` with this node's data, then update the node
      this.update(_.extend({ }, data, this.node))

      currentDataPath = originalDataPath
    }
  })
}

module.exports = function (config, req, res, context) {
  let project = context.project
  let filePath = project.findStyleguideFile(`${context.requestedPath}.json`)

  if (!filePath) {
    return false
  }

  let data = JSON.parse(fs.readFileSync(filePath, 'utf8'))

  // Resolve all _dataUrl paths
  resolveDataUrlPaths(data, filePath, project)

  // Resolve all _template paths.
  resolveTemplatePath(data, filePath, project)

  // Validate the JSON data. Exceptions for the special keys we have that are maps, so they don't need _template or _view
  traverse(data).forEach(function (value) {
    if (_.isPlainObject(value)
            && !value._template
            && !value._view
            && this.key.slice(0, 1) !== '_'
            && this.key !== 'displayOptions'
            && this.key !== 'extraAttributes'
            && this.key !== 'jsonObject') {
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
    var wrapperPath = project.findStyleguideFile(data._wrapper ? data._wrapper : '_wrapper.json')

    while (wrapperPath) {
      var wrapper = JSON.parse(fs.readFileSync(wrapperPath, 'utf8'))

      resolveDataUrlPaths(wrapper, wrapperPath, project)

      resolveTemplatePath(wrapper, wrapperPath, project)

      traverse(wrapper).forEach(function (value) {
        if (value && value._delegate) {
          this.update(data)
        }
      })

      data = wrapper

      wrapperPath = data._wrapper ? project.findStyleguideFile(data._wrapper) : null
    }
  }

  // post-process the JSON data.
  new DataGenerator(context, config).process(data)

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
    var jsonTemplate = fs.readFileSync(context.project.findStyleguideFile('/example-json.hbs'), 'utf8')
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
  var template = handlebars.compile(fs.readFileSync(project.findStyleguideFile('/example-file.hbs'), 'utf8'))

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
  handlebars.registerHelper('extraAttributes', function (context, options) {
    var extraAttributes = ''
    var extraAttributesData

    if (!context) {
      return
    }

    if (!context.data.root.extraAttributes) {
      return
    }

    extraAttributesData = context.data.root.extraAttributes

    for (var key in extraAttributesData) {
      extraAttributes += ' ' + key + '="' + extraAttributesData[key] + '"'
    }

    return new handlebars.SafeString(extraAttributes)
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
      let absolutePath = resolvePath(project._projectPath, extend, null, options.data.root._contextPath || options.data.root._template)
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

  // Renders the named block, optionally replacing its body.
  handlebars.registerHelper('block', function (name, options) {
    var template = compile(name)
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

  var iframeJs = fs.readFileSync(require.resolve('iframe-resizer/js/iframeResizer.contentWindow.min.js'), 'utf8')

  // if the example document contains a </head> then we append the iframe javascript to the <head>
  res.send(template({
    device: req.query.device === 'true',
    data: convert(data)
  }).replace(/<\/head>/, '<script>' + iframeJs + '</script>\n</head>'))

  return res
}
