import React from 'react';
import ReactDOM from 'react-dom';
import { Provider, connect } from 'react-redux'
import { AppContainer } from 'react-hot-loader';
//import { RouterProvider } from 'redux-little-router';

import App from './AppFromRoutes'


let config;
let store;

export const render = (appStore, appConfig) => {
    config = appConfig;
    store = appStore;

    _render(App, store, config);
}

const _render = (Component, store, props) => {
    const Connected = connect((state => ({
        urlParams: state.router.params,
        urlQuery: state.router.query
    })))(Component)

    ReactDOM.render(
        <AppContainer>
            <Provider store={ store }>
                <Connected { ...props } />
            </Provider>
        </AppContainer>,
        document.getElementById('app-wrapper')
    );
};





// Hot Module Replacement API
/*if (module && module.hot) {
    module.hot.accept('./App', () => {
        _render(App, store, config)
    });
}*/

