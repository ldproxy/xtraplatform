import 'babel-polyfill';
import React from 'react';
import ReactDOM from 'react-dom';
import { Provider } from 'react-redux'
import { AppContainer } from 'react-hot-loader';
import { hashHistory as history } from 'react-router'
import { syncHistoryWithStore } from 'react-router-redux'
import createStore from './create-store'
import App from './components/container/App'
import { actions } from './reducers/app'
//console.log(actions);

import services from './assets/services'

const render = (Component, store) => {
    ReactDOM.render(
        <AppContainer>
            <Provider store={ store }>
                <Component history={ syncHistoryWithStore(history, store) } />
            </Provider>
        </AppContainer>,
        document.getElementById('app-wrapper')
    );
};


const store = createStore( /*{
    service: services
}*/ )
store.dispatch(actions.changeTitle('ldproxy'));
document.title = 'ldproxy Manager';

render(App, store);

// Hot Module Replacement API
if (module && module.hot) {
    module.hot.accept('./components/container/App', () => {
        render(App, store)
    });
/*module.hot.accept('./create-store', () => {
    render(App, createStore())
});*/
}
