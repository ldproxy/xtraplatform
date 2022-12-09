import React from 'react';
import PropTypes from 'prop-types';
import { createFeature, fassetValidations } from 'feature-u';
import { validatePropTypes } from '@xtraplatform/core';
import { routes } from '@xtraplatform/manager';

import CodelistIndex from './Listing';
import CodelistDetails from './Details';

const codelistsFeature = 'codelists';

export { codelistsFeature };

export default createFeature({
    name: codelistsFeature,

    fassets: {
        // provided resources
        defineUse: {
            [routes(codelistsFeature)]: [
                {
                    path: '/codelists',
                    menuLabel: 'services/ogc_api:codelists._label',
                    content: <CodelistIndex />,
                    default: true,
                },
                {
                    path: '/codelists/:id',
                    content: <CodelistDetails />,
                },
            ],
        },
    },
});
