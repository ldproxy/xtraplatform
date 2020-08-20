import { render } from 'react-dom';
import { launchApp } from 'feature-u';

import Manager from './Manager';
import ThemeBase, { theme } from './ThemeBase';

const launch = (features) => {
    return launchApp({
        features: [Manager, ThemeBase, ...features],
        aspects: [],
        registerRootAppElm: (app) =>
            // eslint-disable-next-line no-undef
            render(app, document.getElementById('root')),
        showStatus: (msg = '', err = null) => console.log('SPLASH', msg, err),
    });
};

export { theme as themeBase, launch };
