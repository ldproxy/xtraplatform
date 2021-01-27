import React, { useCallback } from 'react';
import PropTypes from 'prop-types';
import { useHistory, useParams } from 'react-router-dom';

import { Content, Async } from '@xtraplatform/core';
import Header from './Header';
import Main from './Main';
import { useServiceAdd, useServices } from '../../../hooks';

const ServiceAdd = () => {
    const history = useHistory();
    const [addService, { loading, error }] = useServiceAdd();
    const { refetch } = useServices();

    const onCancel = () => history.push('/services');
    const onChange = (service) => {
        addService(service).then(() => {
            if (!error) {
                refetch();
                history.push('/services');
            }
        });
    };
    return (
        <Content
            header={<Header loading={loading} onCancel={onCancel} />}
            main={<Main loading={loading} error={error} addService={onChange} />}
        />
    );
};

ServiceAdd.displayName = 'ServiceAdd';

ServiceAdd.propTypes = {};

export default ServiceAdd;
