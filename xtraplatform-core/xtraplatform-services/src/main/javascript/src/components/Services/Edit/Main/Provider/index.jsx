import React from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';

import { Box, Form } from 'grommet';
import { AutoForm, TextField, ToggleField, Async, getFieldsDefault } from '@xtraplatform/core';
import { useProvider } from '@xtraplatform/services';

const ServiceEditProvider = ({ id, api, debounce, onPending, onChange }) => {
    const api2 = api || []; //TODO: why do neither the props nor the deconstruct defaults work?

    // TODO: now only needed for defaults, get merged values from backend
    const mergedBuildingBlocks = {};

    api2.forEach((ext) => {
        const bb = ext.buildingBlock;
        if (mergedBuildingBlocks[bb]) {
            mergedBuildingBlocks[bb] = merge(mergedBuildingBlocks[bb], ext);
        } else {
            mergedBuildingBlocks[bb] = ext;
        }
    });

    const providerId =
        (mergedBuildingBlocks['FEATURES_CORE'] &&
            mergedBuildingBlocks['FEATURES_CORE'].featureProvider) ||
        id;
    const { loading, error, data } = useProvider(providerId);

    const connectionInfo = (data && data.provider && data.provider.connectionInfo) || {};

    const type = data && data.provider && data.provider.providerSubType;
    const nativeCrs = (data && data.provider && data.provider.nativeCrs) || {};

    const { t } = useTranslation();

    return (
        <Async loading={loading} error={error}>
            <Box pad={{ horizontal: 'small', vertical: 'medium' }} fill='horizontal'>
                <Form>
                    <TextField
                        name='type'
                        label={t('services/ogc_api:services.datasource.type')}
                        value={type}
                        readOnly
                    />
                    {type === 'SQL' && (
                        <>
                            {connectionInfo.host && (
                                <TextField
                                    name='host'
                                    label={t('services/ogc_api:services.datasource.host')}
                                    value={connectionInfo.host}
                                    readOnly
                                />
                            )}
                            <TextField
                                name='database'
                                label={t('services/ogc_api:services.datasource.database')}
                                value={connectionInfo.database}
                                readOnly
                            />
                            {connectionInfo.user && (
                                <TextField
                                    name='user'
                                    label={t('services/ogc_api:services.datasource.user')}
                                    value={connectionInfo.user}
                                    readOnly
                                />
                            )}
                            <TextField
                                name='schemas'
                                label={t('services/ogc_api:services.datasource.schemas')}
                                value={connectionInfo.schemas && connectionInfo.schemas.join()}
                                readOnly
                            />
                            <TextField
                                name='nativeCrs'
                                label={t('services/ogc_api:services.datasource.nativeCrs')}
                                value={`${nativeCrs.code}${
                                    nativeCrs.forceAxisOrder && nativeCrs.forceAxisOrder !== 'NONE'
                                        ? ' ' + nativeCrs.forceAxisOrder
                                        : ''
                                }`}
                                readOnly
                            />
                        </>
                    )}
                    {type === 'WFS' && (
                        <>
                            <TextField
                                name='url'
                                label={t('services/ogc_api:services.datasource.url')}
                                value={connectionInfo.uri}
                                readOnly
                            />
                            <TextField
                                name='nativeCrs'
                                label={t('services/ogc_api:services.datasource.nativeCrs')}
                                value={`${nativeCrs.code}${
                                    nativeCrs.forceAxisOrder && nativeCrs.forceAxisOrder !== 'NONE'
                                        ? ' ' + nativeCrs.forceAxisOrder
                                        : ''
                                }`}
                                readOnly
                            />
                        </>
                    )}
                </Form>
            </Box>
        </Async>
    );
};

ServiceEditProvider.displayName = 'ServiceEditProvider';

ServiceEditProvider.propTypes = {
    id: PropTypes.string.isRequired,
    api: PropTypes.array,
    onChange: PropTypes.func.isRequired,
};

export default ServiceEditProvider;
