const fs = require('fs')
const path = require('path')
const rrs = require('recursive-readdir-sync')
const label = require('./label')

function Project (config, projectPath) {
  this._config = config
  this.name = label(path.basename(path.resolve(projectPath)))

  let targetPath = this._config.source

  if (fs.existsSync(projectPath)) {
    this._projectPath = projectPath
  }

  if (fs.existsSync(targetPath) && fs.statSync(targetPath).isDirectory()) {
    this._targetPath = targetPath
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

  filePath = path.join(__dirname, '..', 'styleguide', file)

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

module.exports = Project
