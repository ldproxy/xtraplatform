
import Manager from './components/container/Manager'
import Services from './components/container/Services'
import ServiceIndex from './components/container/ServiceIndex'
import ServiceShow from './components/container/ServiceShow'
import ServiceAdd from './components/container/ServiceAdd'

import { render as renderApp } from './components/common/AppLittleRouter'
import createStore from './create-store'

// TODO: wrap config editing components under ServiceEdit 

export const app = {
    applicationName: 'XtraPlatform',
    serviceTypes: [ /*'base'*/ ],
    routes: {
        path: '/',
        component: Manager,
        title: 'Manager',
        routes: [
            {
                path: '/services',
                component: Services,
                title: 'Services',
                menu: true,
                routes: [
                    {
                        path: '/addcatalog'
                    },
                    {
                        path: '/add',
                        typedComponent: 'ServiceAdd'
                    },
                    {
                        path: '/:id/:ftid'
                    },
                    {
                        path: '/:id',
                        typedComponent: 'ServiceShow'
                    },
                    {
                        path: '/',
                        component: ServiceIndex
                    }
                ]
            }
        ]
    },
    typedComponents: {
        /*ServiceAdd: {
            base: ServiceAdd
        },
        ServiceShow: {
            base: ServiceShow
        }*/
    },
    serviceMenu: [],
    mapStateToProps: state => ({
        urlParams: state.router.params,
        urlQuery: state.router.query,
    //serviceType: state.router.params && state.router.params.id && state.entities.services && state.entities.services[state.router.params.id] && state.entities.services[state.router.params.id].type // || 'base'
    }),
    createStore: createStore
};

let store
let cfg

const initialData = {
    entities: {
        services: {
        }
    }
}

export const render = (mdl) => {
    cfg = mdl
    store = mdl.createStore(mdl.routes, initialData);

    renderApp(store, mdl);
};

// Hot Module Replacement API
if (module && module.hot) {
    module.hot.accept('./module.js', () => {
        renderApp(store, cfg);
    });
}