const findParentDir = require('find-parent-dir')
const fs = require('fs')
const _ = require('lodash')
const path = require('path')
const traverse = require('traverse')

function resolvePath (root, parent, file) {
  if (file.startsWith('/node_modules/')) {
    return path.join(root, file)
  } else if (file.startsWith('/')) {
    const packageRoot = findParentDir.sync(parent, 'package.json')

    if (packageRoot) {
      return path.join(packageRoot, file)
    } else {
      return path.join(root, file)
    }
  } else {
    return path.resolve(path.dirname(parent), file)
  }
}

function resolveData (root, file, extra) {
  const data = JSON.parse(fs.readFileSync(file, 'utf8'))

  if (extra) {
    _.extend(data, extra)
  }

  traverse(data).forEach(function (value) {
    if (this.key === '_template') {
      this.update(resolvePath(root, file, value))
    } else if (value) {
      const include = value._include || value._dataUrl

      if (include) {
        const includeFile = resolvePath(root, file, include)

        if (!fs.existsSync(includeFile)) {
          throw new Error(`Can't include [${include}] from [${file}]! (looked at [${includeFile}])`)
        }

        value._include = null
        value._dataUrl = null
        this.update(resolveData(root, includeFile, value), true)
      }
    }
  })

  return data
}

module.exports = {
  path: resolvePath,
  data: resolveData
}
