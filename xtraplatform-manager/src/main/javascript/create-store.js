import { createStore, applyMiddleware, combineReducers } from 'redux'
import { composeWithDevTools } from 'redux-devtools-extension/developmentOnly';

//import { routerReducer, routerMiddleware } from 'react-router-redux'
//import { hashHistory as history } from 'react-router'

import { routerForBrowser, initializeCurrentLocation, push } from 'redux-little-router';
import { routesToLittleRouter } from './util'
//import { persistStore } from 'redux-persist'
//import { createFilter } from 'redux-persist-transform-filter';
//import createActionBuffer from 'redux-action-buffer'

import createSagaMiddleware from 'redux-saga'
import { reducer as uiReducer } from 'redux-ui'
import { entitiesReducer, queriesReducer, queryMiddleware } from 'redux-query';


import * as reducers from './reducers'
//import rootSaga from './sagas'

export default function(routes, data) {

    const {reducer: routerReducer, middleware: routerMiddleware, enhancer: routerEnhancer} = routerForBrowser({
        routes: routesToLittleRouter(routes),
        basename: '/manager'
    })

    const combine = (reds) => combineReducers({
        ...reds,
        router: routerReducer,
        ui: uiReducer,
        entities: entitiesReducer,
        queries: queriesReducer,
    })

    const reducer = combine(reducers)

    //const routerMiddleware2 = routerMiddleware(history)
    //const sagaMiddleware = createSagaMiddleware()

    const queriesMiddleware = queryMiddleware((state) => state.queries, (state) => state.entities)

    //const initMiddleware = createActionBuffer(appActions.initApp.toString());

    const middleware = [ /*sagaMiddleware,*/ routerMiddleware, queriesMiddleware /*, initMiddleware*/ ];

    // Be sure to ONLY add this middleware in development!
    //if (process.env.NODE_ENV !== 'production')
    //middleware.unshift(require('redux-immutable-state-invariant').default())


    var store = createStore(
        reducer,
        data,
        composeWithDevTools(
            routerEnhancer,
            applyMiddleware(...middleware),
        // other store enhancers if any
        )
    );

    if (module && module.hot) {
        // Enable Webpack hot module replacement for reducers
        module.hot.accept('./reducers', () => {
            const nextReducer = require('./reducers');
            store.replaceReducer(combine(nextReducer));
        });
    }

    //sagaMiddleware.run(rootSaga);

    const initialLocation = store.getState().router;
    if (initialLocation) {
        store.dispatch(initializeCurrentLocation(initialLocation));
    }

    return store
}

/*
const persistingStore = persistStore(store, {
    keyPrefix: 'PMT.',
    debounce: 1000,
    whitelist: ['auth', 'app'],
    transforms: [
        createFilter(
            'app',
            ['useThreePaneView', 'useSmallerFont', 'menuOpen', 'flattenInheritance', 'flattenOninas', 'busy']
        )
    ]
}, () => {
    // ...after creating your store
    const initialLocation = store.getState().router;
    if (initialLocation) {
        //TODO
        setTimeout(function() {
            store.dispatch(initializeCurrentLocation(initialLocation));
        }, 500)

    }
});
*/
