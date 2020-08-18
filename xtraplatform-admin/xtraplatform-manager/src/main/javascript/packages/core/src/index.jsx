import { render } from 'react-dom';
import { launchApp } from 'feature-u';

import {
    Content,
    Sidebar,
    TileGrid,
    Tile,
    StatusIcon,
    SpinnerIcon,
    FormFieldHelp,
    InfoLabel,
    NavLink,
    TaskProgress,
} from './components';
import { Manager, ThemeBase, themeBase } from './features';
import { validatePropTypes } from './featureUtils';
import {
    useQuery,
    useDeepCompareEffect,
    usePrevious,
    useDebounceValue,
    useDebounce,
    useDebounceFields,
    useHover,
} from './hooks';

const launch = (features) => {
    return launchApp({
        features: [Manager, ThemeBase, ...features],
        aspects: [],
        registerRootAppElm: (app) =>
            render(app, document.getElementById('root')),
        showStatus: (msg = '', err = null) => console.log('SPLASH', msg, err),
    });
};

export {
    Content,
    Sidebar,
    TileGrid,
    Tile,
    StatusIcon,
    SpinnerIcon,
    FormFieldHelp,
    InfoLabel,
    NavLink,
    TaskProgress,
    launch,
    themeBase,
    validatePropTypes,
    useQuery,
    useDeepCompareEffect,
    usePrevious,
    useDebounceValue,
    useDebounce,
    useDebounceFields,
    useHover,
};
