import { addons } from '@storybook/addons';
import { themes } from '@storybook/theming';
import { create } from '@storybook/theming/create';

const xtraplatformTheme = create({
    base: 'light',
    brandTitle: '@xtraplatform',
});

addons.setConfig({
    theme: xtraplatformTheme,
});
