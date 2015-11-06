var fs = require('fs');
var path = require('path');
var rrs = require('recursive-readdir-sync');
var traverse = require('traverse');

var rootPath = process.argv[2];

rrs(rootPath).forEach(function (filePath) {
    if (path.extname(filePath) === '.json' && filePath.slice(0, 1) !== '_') {
        var data = JSON.parse(fs.readFileSync(process.argv[3], 'utf8'));

        traverse(data).forEach(function (value) {
            if (value._wrapped) {
                this.update(JSON.parse(fs.readFileSync(filePath, 'utf8')));
            }
        });

        fs.writeFileSync(filePath, JSON.stringify(data, null, '  ') + '\n', 'utf8');
    }
});
