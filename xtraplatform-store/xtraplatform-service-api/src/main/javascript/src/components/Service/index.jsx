import React from 'react';
import { createFeature } from 'feature-u';

import { ServiceIndexHeader, ServiceIndexMain } from './Index';
import { ServiceEditHeader } from './Edit';

const featureName = 'service'

export default createFeature({
  name: featureName,

  fassets: {
    defineUse: { // KEY: supply content under contract of the app feature
      [`${featureName}.routes`]: [
        {
          path: '/services',
          menuLabel: 'Services',
          headerComponent: () => <ServiceIndexHeader serviceTypes={[{ id: 'ogc_api', label: 'OGC API' }]} />,
          mainComponent: () => <ServiceIndexMain services={[{ id: 'flur', label: 'Flurstuecke' }]} />,
        },
        {
          path: '/services/:id',
          headerComponent: ServiceEditHeader,
          mainComponent: () => <div>FLUR</div>,
        }
      ]
    }
  }

});
