import React from 'react';
import { createFeature } from 'feature-u';
import { customTheme } from './theme'

const featureName = 'themedefault'

export { customTheme as theme }

export default createFeature({
  name: featureName,

  fassets: {
    define: { // KEY: supply content under contract of the app feature
      [`default.theme`]: customTheme,
    }
  }

});
