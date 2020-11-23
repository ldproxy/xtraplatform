import { render } from 'react-dom';
import { launchApp } from 'feature-u';

import Manager from './Manager';
import ThemeBase from './ThemeBase';
import ThemeXtraProxy from './ThemeXtraProxy';

const launch = (features) => {
    return launchApp({
        features: [Manager, ThemeBase, ThemeXtraProxy, ...features],
        aspects: [],
        registerRootAppElm: (app) =>
            // eslint-disable-next-line no-undef
            render(app, document.getElementById('root')),
        //showStatus: (msg = '', err = null) => console.log('SPLASH', msg, err),
    });
};

export { launch };
