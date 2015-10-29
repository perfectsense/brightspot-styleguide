function HandlebarsRenderer() {

  var log = require('./logger');
  var fs = require('fs');
  var path = require('path');
  var hbs = require('handlebars');
  var recursiveReadSync = require('recursive-readdir-sync');
  var _ = require('lodash');

  /*
   * This convenience method registers all the partials into Handlebars found under the provided paths.
   * It does so by recursively reading each file from disk and then processing it's filepath to normalize
   * it for the Handlebars registry.
   */
  this.registerPartials = function() {
    var filepaths, partialCount = 0;
    var len = this.config.templatePaths.length;

    /*
     * Looping backwards through the template filepaths is important
     * because it allows us to ask HBS to register overtop existing partials
     * which preserves the file precedence for overrides.
     */
    for (var i=len; i-- > 0;){
      try {
        filepaths = recursiveReadSync(this.config.templatePaths[i]);
        filepaths.map(function(filepath){
          // get the portion of the filepath under the 'render'
          // strip off the leading slash
          // strip off the trailing extension (.hbs)
          var name = filepath.split('render')[1].split(/\/(.*)$/)[1].split('.hbs')[0];
          // get the portion of the filepath rooted at the 'render'
          var relPath = filepath.substr(filepath.lastIndexOf('render'))
          // register this partial with Handlebars
          hbs.registerPartial(name, fs.readFileSync(filepath, "utf8"));
          partialCount++;
          return {"name": name, "relPath": relPath};
        });
      } catch(err){
        // log.error('Path does not exist');
      }
    }

    if (partialCount){
      // log.success('Registered: '+ partialCount +' Partials');
    }
  }

  this.init = function(config){
    this.config = config;

    // Handles rendering of partials ("_template" strings)
    hbs.registerHelper('render', function(context, fullScope) {
      if (!context) {
        return '';
      }

      if (typeof context !== 'object') {
        return context.toString();
      }

      context = _.extend({ }, context, fullScope.hash);
      var partial = hbs.partials[context['_template']];
      var template = hbs.compile(partial);
      var hydrated = template(context);
      return new hbs.SafeString(hydrated);
    });
  }

}

// export as a singleton
module.exports = exports = new HandlebarsRenderer();
