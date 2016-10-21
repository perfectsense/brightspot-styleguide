let Styleguide = {
    distPath() {
        return '_dist'
    },

    serve(args) {
        return require('./lib/server')(args)
    }
}

module.exports = Styleguide;
