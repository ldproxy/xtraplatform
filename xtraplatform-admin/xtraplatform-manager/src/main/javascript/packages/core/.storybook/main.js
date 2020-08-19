const util = require('util');

module.exports = {
    stories: [
        '../src/**/*.@(stories|story).mdx',
        '../src/**/stories.mdx',
        '../src/**/*.stories.@(js|jsx|ts|tsx)',
        '../src/**/stories.@(js|jsx|ts|tsx)',
    ],
    addons: [
        //    "@storybook/addon-actions",
        '@storybook/addon-links',
        //    "@storybook/addon-docs",
        '@storybook/addon-essentials',
    ],
    webpackFinal: (config) => {
        config.module.rules[0].exclude = /node_modules(?!(\/|\\)@xtraplatform)/;
        config.module.rules[0].include = '/home/zahnen/development/geo_json';
        console.log(util.inspect(config, false, null, true));
        return config;
    },
};
