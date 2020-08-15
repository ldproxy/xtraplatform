import React from 'react';
import PropTypes from 'prop-types';
import { useParams } from "react-router-dom";

import { Content } from '@xtraplatform/core';
import ServiceEditHeader from './Header';
import ServiceEditMain from './Main';

const ServiceEdit = ({ }) => {
    const { id } = useParams();

    const service = {
        id: id,
        label: id.toUpperCase(),
        hasBackgroundTask: true,
        progress: 10,
        message: 'bla',
    }

    const onChange = (change) => { console.log('CHANGE', change) }

    return (
        <Content
            header={
                <ServiceEditHeader service={service} />
            }
            main={
                <ServiceEditMain service={service} onChange={onChange} />
            } />
    )
}


ServiceEdit.displayName = 'ServiceEdit';

ServiceEdit.propTypes = {
    isCompact: PropTypes.bool,
};

export default ServiceEdit;
