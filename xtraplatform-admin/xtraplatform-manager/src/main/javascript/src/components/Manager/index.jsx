import React from 'react';
import PropTypes from 'prop-types';
import { createFeature, assertNoRootAppElm } from 'feature-u';

import Manager from './Container';


const assetTypes = {
    'routes': {
        path: PropTypes.string.isRequired,
        menuLabel: PropTypes.string,
        headerComponent: PropTypes.func.isRequired,
        mainComponent: PropTypes.func.isRequired,
    },
    'theme': {
    }
}

export default createFeature({
    name: 'manager',
    fassets: {
        use: Object.keys(assetTypes).map(assetType => `*.${assetType}`) // our usage contract
    },
    appWillStart: ({ fassets: featureAssets, curRootAppElm }) => {
        // ensure fassets comply to schema
        Object.keys(assetTypes).forEach(assetType => {
            Object.keys(featureAssets).forEach(featureAsset => {
                if (featureAssets[featureAsset] instanceof Object && featureAssets[featureAsset].hasOwnProperty(assetType)) {
                    const asset = featureAssets[featureAsset][assetType]
                    if (Array.isArray(asset)) {
                        asset.forEach(a => PropTypes.checkPropTypes(assetTypes[assetType], a, 'fasset prop', `${featureAsset}.${assetType}`))
                    } else {
                        PropTypes.checkPropTypes(assetTypes[assetType], asset, 'fasset prop', `${featureAsset}.${assetType}`);
                    }
                }
            })
        })

        // ensure no content is clobbered (children NOT supported)
        assertNoRootAppElm(curRootAppElm, '<Manager>');

        return <Manager />;
    }
});
