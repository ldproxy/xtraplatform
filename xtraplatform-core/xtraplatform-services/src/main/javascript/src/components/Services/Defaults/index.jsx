import React, { useCallback, useState } from 'react';
import PropTypes from 'prop-types';
import { useHistory, useParams } from 'react-router-dom';

import { Content, Async } from '@xtraplatform/core';
import Header from './Header';
import Main from './Main';
import { useServiceDefaults, useServiceDefaultsPatch, patchDebounce } from '../../../hooks';

const ServiceEditDefaults = () => {
    const history = useHistory();
    const { loading, error, data } = useServiceDefaults();
    const [
        patchDefaults,
        { loading: mutationLoading, error: mutationError, data: mutationSuccess },
    ] = useServiceDefaultsPatch();
    const [mutationPending, setPending] = useState(false);

    const onCancel = () => history.push('/services');

    const onPending = () => setPending(true);

    const onChange = useCallback(
        (finalChanges) => {
            if (Object.keys(finalChanges).length > 0) {
                patchDefaults(finalChanges);
                setPending(false);
            }
        },
        [patchDefaults]
    );

    const defaults = data ? data.defaults : {};

    return (
        <Content
            header={
                <Header
                    mutationPending={mutationPending}
                    mutationLoading={mutationLoading}
                    mutationError={mutationError}
                    mutationSuccess={mutationSuccess}
                    onCancel={onCancel}
                />
            }
            main={
                <Async loading={loading} error={error}>
                    <Main
                        service={defaults}
                        debounce={patchDebounce}
                        onPending={onPending}
                        onChange={onChange}
                    />
                </Async>
            }
        />
    );
};

ServiceEditDefaults.displayName = 'ServiceEditDefaults';

ServiceEditDefaults.propTypes = {};

export default ServiceEditDefaults;
