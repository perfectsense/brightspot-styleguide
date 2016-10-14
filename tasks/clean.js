var del = require('del');

module.exports = function(gulp){
    // A clean-up task for the _dist directory
    gulp.task('clean:dist', function(callback) {
        return del(['_dist/**/*']);
    });

    // A clean-up task for the _tmp directory
    gulp.task('clean:tmp', function(callback) {
        return del(['_tmp/**/*']);
    });
}
