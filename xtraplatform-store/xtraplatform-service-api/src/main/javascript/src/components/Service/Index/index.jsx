import React from 'react';
import PropTypes from 'prop-types';
import { useParams } from "react-router-dom";

import { Content } from '@xtraplatform/core';
import ServiceIndexHeader from './Header';
import ServiceIndexMain from './Main';

const ServiceIndex = ({ isCompact }) => {

    const serviceTypes = [{ id: 'ogc_api', label: 'OGC API' }];
    const services = [{ id: 'flur', label: 'Flurstuecke' }];

    return (
        <Content
            header={
                <ServiceIndexHeader serviceTypes={serviceTypes} isCompact={isCompact} />
            }
            main={
                <ServiceIndexMain services={services} isCompact={isCompact} />
            } />
    )
}


ServiceIndex.displayName = 'ServiceIndex';

ServiceIndex.propTypes = {
    isCompact: PropTypes.bool,
};

export default ServiceIndex;
