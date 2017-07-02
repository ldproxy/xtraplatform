
import { render } from './components/common/AppLittleRouter'

import Manager from './components/container/Manager'
import Services from './components/container/Services'
import ServiceShow from './components/container/ServiceShow'
import ServiceAdd from './components/container/ServiceAdd'



const app = {
    applicationName: 'XtraPlatform',
    routes: {
        path: '/',
        component: Manager,
        title: 'Manager',
        routes: [
            {
                path: '/services/add',
                component: ServiceAdd,
                parent: '/services'
            },
            {
                path: '/services/:id',
                component: ServiceShow,
                parent: '/services'
            },
            {
                path: '/services/:id/:ftid',
                parent: '/services/:id'
            },
            {
                path: '/services',
                component: Services,
                title: 'Services',
                menu: true
            }
        ]
    }
}

render(app);

// Hot Module Replacement API
if (module && module.hot) {
    module.hot.accept('./index.jsx', () => {
        render(app);
    });
}