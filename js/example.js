const _ = require('lodash')
const findParentDir = require('find-parent-dir')
const fs = require('fs')
const path = require('path')
const traverse = require('traverse')

const resolver = require('./resolver')

module.exports = function (styleguide, filePath) {
  const buildPath = styleguide.path.build()
  let data = resolver.data(buildPath, filePath)
  const displayName = data._displayName

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

  return {
    designs: data.body ? data.body._designs : null,
    displayName: displayName,
    html: styleguide.handlebars(data)
  }
}
