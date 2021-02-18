import React, { useCallback, useState } from 'react';
import PropTypes from 'prop-types';
import { useParams, useHistory } from 'react-router-dom';

import { Content, Async } from '@xtraplatform/core';
import ServiceEditHeader from './Header';
import ServiceEditMain from './Main';
import {
    useService,
    useServices,
    useServiceStatus,
    useServicePatch,
    useServiceDefaults,
    patchDebounce,
    useServiceDelete,
} from '../../../hooks';

const ServiceEdit = () => {
    const { id } = useParams();
    const history = useHistory();
    const { loading, error, data } = useServiceStatus(id);
    const { loading: loading2, error: error2, data: data2 } = useService(id);
    const { loading: loading3, error: error3, data: data3 } = useServiceDefaults();
    const [
        patchService,
        { loading: mutationLoading, error: mutationError, data: mutationSuccess },
    ] = useServicePatch(id);
    const [deleteService] = useServiceDelete(id);
    const { refetch } = useServices();
    const [mutationPending, setPending] = useState(false);

    const onPending = () => setPending(true);

    const onChange = useCallback(
        (finalChanges) => {
            if (Object.keys(finalChanges).length > 0) {
                patchService(finalChanges);
                setPending(false);
            }
        },
        [patchService]
    );

    const onDelete = () => {
        deleteService().then(() => {
            //setLayerOpened(false);
            //setDeletePending(false);
            setTimeout(() => {
                refetch();
                history.push('/services');
            }, 2000);
        });
    };

    const status = data ? data.status : {};
    const service = data2 ? data2.service : {};
    const defaults = data3 ? data3.defaults : {};

    return (
        <Async loading={loading} error={error} noSpinner>
            <Content
                header={
                    <ServiceEditHeader
                        service={status}
                        mutationPending={mutationPending}
                        mutationLoading={mutationLoading}
                        mutationError={mutationError}
                        mutationSuccess={mutationSuccess}
                        onChange={onChange}
                        onDelete={onDelete}
                    />
                }
                main={
                    <Async loading={loading2 || loading3} error={error2 || error3}>
                        <ServiceEditMain
                            service={service}
                            defaults={defaults}
                            debounce={patchDebounce}
                            onPending={onPending}
                            onChange={onChange}
                        />
                    </Async>
                }
            />
        </Async>
    );
};

ServiceEdit.displayName = 'ServiceEdit';

ServiceEdit.propTypes = {};

export default ServiceEdit;
