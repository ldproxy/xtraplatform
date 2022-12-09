import React, {useEffect} from 'react';
import PropTypes from 'prop-types';

import { Content, Async } from '@xtraplatform/core';
import ServiceIndexHeader from './Header';
import ServiceIndexMain from './Main';
import { useServices } from '../../../hooks';

const ServiceIndex = ({ isCompact }) => {
    const serviceTypes = [{ id: 'ogc_api', label: 'OGC API' }];

    const { loading, error, data, startPolling, stopPolling } = useServices();
    const services = data ? data.services : [];

    const poll = services.some(status => status.hasBackgroundTask)

    useEffect(() => {
        if (poll) {
            startPolling(1000)

            return stopPolling
        }
        return () => {}
    }, [poll, startPolling, stopPolling])

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
