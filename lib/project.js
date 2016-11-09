const fs = require('fs')
const path = require('path')
const rrs = require('recursive-readdir-sync')
const parseString = require('xml2js').parseString
const label = require('./label')

function Project (config, projectPath) {
  var self = this

  this._config = config
  this.name = label(path.basename(path.resolve(projectPath)))

  if (fs.existsSync(projectPath)) {
    this._projectPath = projectPath
  }

  var projectSrcPath = config['project-src-path']
  if (projectSrcPath) {
    projectSrcPath = path.join(this._projectPath, config['project-src-path'])
  }

  if (fs.existsSync(projectSrcPath) && fs.statSync(projectSrcPath).isDirectory()) {
    this._projectSrcPath = projectSrcPath
  }

  if (config['project-path'] === projectPath) {
    var pomPath = path.join(projectPath, config['maven-pom'])

    if (fs.existsSync(pomPath)) {
      parseString(fs.readFileSync(pomPath), { async: false }, function (err, pomXml) {
        var targetName = pomXml.project.artifactId + '-' + pomXml.project.version
        var targetPath = path.join(projectPath, 'target', targetName)

        if (fs.existsSync(targetPath)) {
          self._targetPath = targetPath
        }
      })
    }
  }
}

Project.prototype.forEachPath = function (callback) {
  if (this._projectSrcPath) {
    callback(this._projectSrcPath)
  }

  if (this._targetPath) {
    callback(this._targetPath)
  }

  if (this._projectPath) {
    callback(this._projectPath)
  }
}

Project.prototype.forEachStyleguideItem = function (callback) {
  let projectSrcPath = this._projectSrcPath

  function excludeFilter (group) {
    if (group.slice(0, 1) === '_' ||
            group === 'bower_components' ||
            group === 'node_modules') {
      return false
    }
    return true
  }

  if (projectSrcPath) {
    fs.readdirSync(projectSrcPath).filter(excludeFilter).forEach(function (group) {
      var groupPath = path.join(projectSrcPath, group)

      if (fs.statSync(groupPath).isDirectory() || path.extname(groupPath) === '.json') {
        callback(group, groupPath)
      }
    })
  }
}

// Resolve a brightspot-styleguide file path
Project.prototype.findStyleguideFile = function (file) {
  let filePath
  let basePath = this._projectSrcPath

  if (basePath) {
    filePath = path.join(basePath, file)

    if (fs.existsSync(filePath)) {
      return filePath
    }
  }

  filePath = path.join(__dirname, '..', this._config['styleguide-path'], file)

  if (fs.existsSync(filePath)) {
    return filePath
  }

  return null
}

Project.prototype.findVariableFiles = function () {
    // Find all variable files.
  var varFiles = [ ]
  var projectSrcPath = this._projectSrcPath

  if (projectSrcPath) {
    rrs(projectSrcPath).forEach(function (filePath) {
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
