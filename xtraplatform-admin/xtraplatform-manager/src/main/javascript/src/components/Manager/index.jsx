import React from 'react';
import { createFeature, assertNoRootAppElm } from 'feature-u';
import Manager from './Container';

export default createFeature({
  name: 'manager',
  fassets: {          // NEW:
    use: [            // our usage contract
      '*.menulink',  // ... link components
      '*.route',  // ... route components
      '*.theme',
    ]
  },
  appWillStart: ({ fassets, curRootAppElm }) => {
    assertNoRootAppElm(curRootAppElm, '<Manager>'); // insure no content is clobbered (children NOT supported)
    return <Manager />;
  }
});
