import React from 'react';
import PropTypes from 'prop-types';
import { useFassets } from 'feature-u';

import { Box } from 'grommet';
import { Globe } from 'grommet-icons';
import { Header, TaskProgress, AsyncIcon } from '@xtraplatform/core';
import { serviceViewActions } from '../../constants';
import Actions from './Actions';

const ServiceEditHeader = ({
    service,
    mutationPending,
    mutationLoading,
    mutationError,
    mutationSuccess,
    onChange,
    onDelete,
}) => {
    const ViewActions = useFassets(serviceViewActions());

    const token = null;

    return (
        <Header
            icon={<Globe />}
            label={service.label}
            title={`${service.label} [${service.id}]`}
            actions2={
                <AsyncIcon
                    size='list'
                    pending={mutationPending}
                    loading={mutationLoading}
                    success={mutationSuccess}
                    error={mutationError}
                />
            }
            actions={
                <>
                    {service.hasBackgroundTask && (
                        <Box background='light-2' basis='1/4' flex={service.hasProgress ? 'grow' : 'shrink'} pad='xsmall' round='xsmall'>
                            <TaskProgress hasProgress={service.hasProgress} progress={service.progress} message={service.message} truncate />
                        </Box>
                    )}
                    <Actions
                        id={service.id}
                        status={service.status}
                        enabled={service.enabled}
                        secured={service.secured}
                        token={token}
                        updateService={onChange}
                        removeService={onDelete}
                        ViewActions={ViewActions}
                    />
                </>
            }
        />
    );
};

ServiceEditHeader.displayName = 'ServiceEditHeader';

ServiceEditHeader.propTypes = {
    compact: PropTypes.bool,
    role: PropTypes.string,
};

export default ServiceEditHeader;
