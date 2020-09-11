module.exports = {
    stories: [
        '../xtraplatform-core/xtraplatform-manager/src/main/javascript/**/*.@(stories|story).mdx',
        '../xtraplatform-core/xtraplatform-manager/src/main/javascript/**/stories.mdx',
        '../xtraplatform-core/xtraplatform-manager/src/main/javascript/**/*.stories.@(js|jsx|ts|tsx)',
        '../xtraplatform-core/xtraplatform-manager/src/main/javascript/**/stories.@(js|jsx|ts|tsx)',
    ],
    addons: ['@storybook/addon-links', '@storybook/addon-essentials'],
};
