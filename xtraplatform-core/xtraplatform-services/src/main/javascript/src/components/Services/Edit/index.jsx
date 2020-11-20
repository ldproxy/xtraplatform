import React, { useCallback, useState } from 'react';
import PropTypes from 'prop-types';
import { useParams } from 'react-router-dom';

import { Content, Async, useDebounce } from '@xtraplatform/core';
import ServiceEditHeader from './Header';
import ServiceEditMain from './Main';
import {
    useService,
    useServiceStatus,
    useServicePatch,
    useServiceDefaults,
    patchDebounce,
    useServiceDelete,
} from '../../../hooks';

const ServiceEdit = () => {
    const { id } = useParams();
    const { loading, error, data } = useServiceStatus(id);
    const { loading: loading2, error: error2, data: data2 } = useService(id);
    const { loading: loading3, error: error3, data: data3 } = useServiceDefaults();
    const [
        patchService,
        { loading: mutationLoading, error: mutationError, data: mutationSuccess },
    ] = useServicePatch(id);
    const [deleteService] = useServiceDelete(id);
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
                        onDelete={deleteService}
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
