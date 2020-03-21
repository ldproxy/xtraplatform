import React from 'react';
import { createFeature } from 'feature-u';

const featureName = 'webservice'
const link = () => <div>Link</div>;
const header = () => <div>Services</div>;
const main = () => <div>ServiceList</div>;

export default createFeature({
  name: featureName,

  fassets: {
    defineUse: { // KEY: supply content under contract of the app feature
      [`${featureName}.menulink`]: link,
      [`${featureName}.route`]: {
        info: {
          title: 'Services',
          path: '/services',
        },
        components: {
          header: header,
          main: main,
        }
      }
    }
  }

});
