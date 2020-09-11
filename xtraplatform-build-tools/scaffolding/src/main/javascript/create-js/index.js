#!/usr/bin/env node

const yeoman = require('yeoman-environment');

const env = yeoman.createEnv();

env.register(
    require.resolve('./generators/setup.js'),
    'xtraplatform:create-package'
);

env.run('xtraplatform:create-package ' + process.argv.slice(2).join(' '));
