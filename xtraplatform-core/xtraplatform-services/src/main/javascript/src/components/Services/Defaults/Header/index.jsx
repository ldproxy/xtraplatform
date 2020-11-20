import React from 'react';
import PropTypes from 'prop-types';

import { Box, Anchor } from 'grommet';
import { SettingsOption, Close } from 'grommet-icons';
import { Header, AsyncIcon } from '@xtraplatform/core';

const ServiceDefaultsHeader = ({
    mutationPending,
    mutationLoading,
    mutationError,
    mutationSuccess,
    onCancel,
}) => {
    return (
        <Header
            icon={<SettingsOption />}
            label='Service Defaults'
            actions2={
                <AsyncIcon
                    size='small'
                    pending={mutationPending}
                    loading={mutationLoading}
                    success={mutationSuccess}
                    error={mutationError}
                />
            }
            actions={
                <Box direction='row'>
                    <Anchor icon={<Close />} title='Back to services' onClick={onCancel} />
                </Box>
            }
        />
    );
};

ServiceDefaultsHeader.displayName = 'ServiceDefaultsHeader';

ServiceDefaultsHeader.propTypes = {
    onCancel: PropTypes.func,
};

export default ServiceDefaultsHeader;
