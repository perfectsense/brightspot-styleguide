const fs = require('fs')
const path = require('path')
const rrs = require('recursive-readdir-sync')
const label = require('./label')

function Project (config, projectPath) {
  this._config = config
  this.name = label(path.basename(path.resolve(projectPath)))

  let targetPath = path.join(this._config['project-target-path'], this._config['project-styleguide-dirname'])

  if (fs.existsSync(projectPath)) {
    this._projectPath = projectPath
  }

  if (fs.existsSync(targetPath) && fs.statSync(targetPath).isDirectory()) {
    this._targetPath = targetPath
  }
}

Project.prototype.forEachPath = function (callback) {
  if (this._targetPath) {
    callback(this._targetPath)
  }

  if (this._targetPath) {
    callback(this._targetPath)
  }

  if (this._projectPath) {
    callback(this._projectPath)
  }
}

Project.prototype.forEachStyleguideItem = function (callback) {
  let targetPath = this._targetPath

  function excludeFilter (group) {
    if (group.slice(0, 1) === '_' ||
            group === 'bower_components' ||
            group === 'node_modules') {
      return false
    }
    return true
  }

  if (targetPath) {
    fs.readdirSync(targetPath).filter(excludeFilter).forEach(function (group) {
      var groupPath = path.join(targetPath, group)

      if (fs.statSync(groupPath).isDirectory() || path.extname(groupPath) === '.json') {
        callback(group, groupPath)
      }
    })
  }
}

// Resolve a brightspot-styleguide file path
Project.prototype.findStyleguideFile = function (file) {
  let filePath
  let basePath = this._targetPath

  filePath = path.join(__dirname, '..', this._config['styleguide-path'], file)

  if (fs.existsSync(filePath)) {
    return filePath
  }

  if (basePath) {
    filePath = path.join(basePath, file)

    if (fs.existsSync(filePath)) {
      return filePath
    }
  }

  return null
}

Project.prototype.findVariableFiles = function () {
    // Find all variable files.
  var varFiles = [ ]
  var targetPath = this._targetPath

  if (targetPath) {
    rrs(targetPath).forEach(function (filePath) {
      if (path.extname(filePath) === '.vars') {
        varFiles.push({
          path: filePath,
          name: filePath.slice(0, -5).split(fs.sep).join('/')
        })
      }
    })
  }

    // Strip the common prefix from the variable file names.
  var varFilesLength = varFiles.length

  if (varFilesLength > 1) {
    varFiles.sort(function (x, y) {
      return x.name.localeCompare(y.name)
    })

    var first = varFiles[0]
    var last = varFiles[varFilesLength - 1]
    var commonLength = first.name.length
    var commonIndex = 0

    while (commonIndex < commonLength && first.name.charAt(commonIndex) === last.name.charAt(commonIndex)) {
      ++commonIndex
    }

    var commonPrefixLength = first.name.substring(0, commonIndex).length

    varFiles.forEach(function (varFile) {
      varFile.name = varFile.name.substring(commonPrefixLength)
    })
  } else if (varFilesLength > 0) {
    varFiles[0].name = path.basename(varFiles[0].name)
  }

  return varFiles
}

module.exports = Project
