import React from 'react';
import PropTypes from 'prop-types';
import { createFeature, fassetValidations } from 'feature-u';
import { validatePropTypes } from '@xtraplatform/core';
import { routes } from '@xtraplatform/manager';

import { servicesFeature, serviceViewActions, serviceEditTabs } from './constants';
import ServiceIndex from './Index';
import ServiceEdit from './Edit';
import ServiceEditGeneral from './Edit/Main/General';
import ServiceDefaults from './Defaults';
import ServiceAdd from './Add';
import ViewActionLandingPage from '../ViewActionLandingPage';

export { servicesFeature, serviceViewActions, serviceEditTabs };

export default createFeature({
    name: servicesFeature,

    appInit: ({ showStatus }) => {
        showStatus('Loading services...');
        return Promise.resolve();
    },

    fassets: {
        // provided resources
        defineUse: {
            [routes(servicesFeature)]: [
                {
                    path: '/services',
                    menuLabel: 'Services',
                    content: <ServiceIndex />,
                    default: true,
                },
                {
                    path: '/services/_defaults',
                    content: <ServiceDefaults />,
                },
                {
                    path: '/services/_add',
                    content: <ServiceAdd />,
                },
                {
                    path: '/services/:id',
                    content: <ServiceEdit />,
                    sidebar: <ServiceIndex isCompact />,
                },
            ],
            [serviceViewActions('landingPage')]: ViewActionLandingPage,
            [serviceEditTabs('general')]: {
                id: 'general',
                label: 'General',
                component: ServiceEditGeneral,
            },
        },
        // consumed resources
        use: [
            [serviceViewActions(), { required: false, type: fassetValidations.comp }],
            [
                serviceEditTabs(),
                {
                    required: false,
                    type: validatePropTypes({
                        id: PropTypes.string.isRequired,
                        label: PropTypes.string.isRequired,
                        component: PropTypes.elementType.isRequired,
                        noDefaults: PropTypes.bool,
                    }),
                },
            ],
        ],
    },
});
