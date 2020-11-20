import React from 'react';
import PropTypes from 'prop-types';

import { Content, Async } from '@xtraplatform/core';
import ServiceIndexHeader from './Header';
import ServiceIndexMain from './Main';
import { useServices } from '../../../hooks';

const ServiceIndex = ({ isCompact }) => {
    const serviceTypes = [{ id: 'ogc_api', label: 'OGC API' }];

    // TODO: set pollInterval when service is being added
    const { loading, error, data } = useServices();
    const services = data ? data.services : [];

    return (
        <Content
            header={<ServiceIndexHeader serviceTypes={serviceTypes} isCompact={isCompact} />}
            main={
                <Async loading={loading} error={error}>
                    <ServiceIndexMain services={services} isCompact={isCompact} />
                </Async>
            }
        />
    );
};

ServiceIndex.displayName = 'ServiceIndex';

ServiceIndex.propTypes = {
    isCompact: PropTypes.bool,
};

export default ServiceIndex;
