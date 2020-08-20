import React from 'react';
import PropTypes from 'prop-types';
import { createFeature, assertNoRootAppElm } from 'feature-u';

import { validatePropTypes } from '@xtraplatform/core';
import Manager from './Container';

export default createFeature({
    name: 'manager',
    fassets: {
        // consumed resources
        use: [
            [
                '*.routes',
                {
                    type: validatePropTypes({
                        path: PropTypes.string.isRequired,
                        content: PropTypes.element.isRequired,
                        menuLabel: PropTypes.string,
                        sidebar: PropTypes.element,
                    }),
                },
            ],
            ['*.theme', {}],
        ],
    },
    appInit: ({ showStatus }) => {
        showStatus('Initializing...');

        return Promise.resolve();
    },
    // eslint-disable-next-line react/prop-types
    appWillStart: ({ curRootAppElm }) => {
        // ensure no content is clobbered (children NOT supported)
        assertNoRootAppElm(curRootAppElm, '<Manager>');

        // return root app
        return <Manager />;
    },
});
