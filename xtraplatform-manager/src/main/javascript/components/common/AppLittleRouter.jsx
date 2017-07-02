import React, { Component } from 'react';
import ReactDOM from 'react-dom';
import { Provider, connect } from 'react-redux'
import { AppContainer } from 'react-hot-loader';
import { RouterProvider, Fragment } from 'redux-little-router';

import createStore from '../../create-store'
import App from '../common/AppFromRoutes'


let config;
let store;

export const render = (appConfig) => {
    config = appConfig;

    store = createStore(appConfig.routes);

    _render(App, store, appConfig);
}

const _render = (Component, store, props) => {
    const Connected = connect(state => ({
        urlParams: state.router.params
    }))(Component)

    ReactDOM.render(
        <AppContainer>
            <Provider store={ store }>
                <RouterProvider store={ store }>
                    <Connected { ...props } />
                </RouterProvider>
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

