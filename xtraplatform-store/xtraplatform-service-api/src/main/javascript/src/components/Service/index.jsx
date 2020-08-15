import React from 'react';
import PropTypes from 'prop-types';
import { createFeature, fassetValidations } from 'feature-u';
import { validatePropTypes } from '@xtraplatform/core'

import { servicesFeature, serviceViewActions, serviceEditTabs } from './constants'
import ServiceIndex from './Index';
import ServiceEdit from './Edit';

export { servicesFeature, serviceViewActions, serviceEditTabs }

export default createFeature({
  name: servicesFeature,

  fassets: {
    // provided resources
    defineUse: {
      [`${servicesFeature}.routes`]: [
        {
          path: '/services',
          menuLabel: 'Services',
          content: <ServiceIndex />,
        },
        {
          path: '/services/:id',
          content: <ServiceEdit />,
          sidebar: <ServiceIndex isCompact={true} />,
        }
      ],
      [serviceViewActions('noop')]: () => <div>noop</div>,
      [serviceEditTabs('noop')]: {
        id: 'noop',
        label: 'noop',
        component: () => <div>noop</div>
      }
    },
    //consumed resources
    use: [
      [serviceViewActions(), { required: false, type: fassetValidations.comp }],
      [serviceEditTabs(), {
        required: false,
        type: validatePropTypes({
          id: PropTypes.string.isRequired,
          label: PropTypes.string.isRequired,
          component: PropTypes.elementType.isRequired
        })
      }],
    ]
  }

});
