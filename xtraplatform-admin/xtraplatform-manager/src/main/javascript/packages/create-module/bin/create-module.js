var yeoman = require('yeoman-environment');
var env = yeoman.createEnv();

env.register(require.resolve('../generators/xtraplatform/index.js'), 'xtraplatform:create-module');

let args = ''

for (let j = 2; j < process.argv.length; j++) {
    args += process.argv[j] + ' ';
}

env.run('xtraplatform:create-module ' + args);

