import { render } from 'react-dom';
import { launchApp } from 'feature-u';
import { App, Service, Theme } from './components';

// launch our app, exposing the Fassets object (facilitating cross-feature-communication)
export default launchApp({
    features: [App, Service, Theme],
    aspects: [],
    registerRootAppElm: (app) => render(app, document.getElementById('root')),
});
