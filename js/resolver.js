const findParentDir = require('find-parent-dir')
const fs = require('fs')
const _ = require('lodash')
const path = require('path')
const traverse = require('traverse')

function resolvePath(root, parent, file) {
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

function resolveData(root, file) {
  let data = JSON.parse(fs.readFileSync(file, 'utf8'))

  traverse(data).forEach(function (value) {
    if (this.key === '_template') {
      this.update(resolvePath(root, file, value))

    } else {
      const dataUrl = value._dataUrl

      if (dataUrl) {
        var dataFile = resolvePath(root, file, dataUrl)

        if (!fs.existsSync(dataFile)) {
          throw new Error(`Can't include [${dataUrl}] from [${file}]! (looked at [${dataFile}])`)
        }

        this.update(_.extend({ }, resolveData(root, dataFile), value), true)
      }
    }
  })

  return data
}

module.exports = {
  path: resolvePath,
  data: resolveData
}
