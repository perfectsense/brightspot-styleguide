const lesshint = require('gulp-lesshint');

module.exports = {
    registerModule: (styleguide) => {
        styleguide.gulp.task('lint:less', () => {
            return styleguide.gulp.src(styleguide.path.src('All.less'))
                .pipe(lesshint({}))
                .pipe(lesshint.reporter(''));
        });

        styleguide.gulp.task('lint:js', () => {
            return
        });

        styleguide.gulp.task('lint:json', () => {
            return
        });
    }
}
