var term = require('terminal-kit').terminal;

module.exports = {
    welcome: function () {
        term.blue(' _____ _____ _____ _____ _____ _____ _____ _____').red(' _____ ').blue('_____ \n');
        term.blue('| __  | __  |     |   __|  |  |_   _|   __|  _  ').red('|     |').blue('_   _|\n');
        term.blue('| __ -|    -|-   -|  |  |     | | | |__   |   __').red('|  |  |').blue(' | |  \n');
        term.blue('|_____|__|__|_____|_____|__|__| |_| |_____|__|  ').red('|_____|').blue(' |_|  \n');
        term.blue('            _____ _       _             _   _     \n');
        term.blue('           |   __| |_ _ _| |___ ___ _ _|_|_| |___ \n');
        term.blue('           |__   |  _| | | | -_| . | | | | . | -_|\n');
        term.blue('           |_____|_| |_  |_|___|_  |___|_|___|___|\n');
        term.blue('                     |___|     |___|              \n');
        term.defaultColor('\n');
    },

    success: function (message) {
        term.green(message + '\n');
        term.defaultColor();
    },

    error: function (message) {
        term.red(message + '\n');
        term.defaultColor();
    }
};
