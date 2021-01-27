import { createFeature } from 'feature-u';
import { createTheme } from '@xtraplatform/core';

import themeXtraProxy from './theme';
import { theme } from '../../feature-u';

export default createFeature({
    name: 'theme-xtraproxy',

    fassets: {
        // provided resources
        define: {
            // KEY: supply content under contract of the app feature
            [theme('xtraproxy')]: createTheme(themeXtraProxy),
        },
    },
});
