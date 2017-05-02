const _ = require('lodash')
const findParentDir = require('find-parent-dir')
const fs = require('fs')
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

function traverseData (root, file, data) {
  return traverse(data).forEach(function (value) {
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

        let resolved = resolveData(root, includeFile, value)

        if (Array.isArray(resolved)) {
          if (value._random) {
            resolved = resolved[Math.floor(Math.random() * resolved.length)]
          } else {
            const key = value._key

            if (key !== undefined && key !== null) {
              resolved = resolved[parseInt(key, 10)]
            }
          }
        }

        const traversed = traverseData(root, file, value)

        if (Array.isArray(resolved)) {
          this.update(resolved.map(i => _.extend(i, traversed)), true)
        } else if (typeof resolved === 'object') {
          this.update(_.extend(resolved, traversed), true)
        } else {
          this.update(resolved, true)
        }
      }
    }
  })
}

function resolveData (root, file) {
  return traverseData(root, file, JSON.parse(fs.readFileSync(file, 'utf8')))
}

module.exports = {
  path: resolvePath,
  data: resolveData
}
