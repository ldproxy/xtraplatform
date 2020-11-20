import React from 'react';
import PropTypes from 'prop-types';

import { Box, Anchor } from 'grommet';
import { ChapterAdd, Close } from 'grommet-icons';
import { Header, SpinnerIcon } from '@xtraplatform/core';

const ServiceAddHeader = ({ loading, onCancel }) => {
    return (
        <Header
            icon={<ChapterAdd />}
            label='Add New Service'
            actions={
                <Box direction='row'>
                    {loading ? (
                        <Box pad='small'>
                            <SpinnerIcon size='medium' />
                        </Box>
                    ) : (
                        <Anchor icon={<Close />} title='Back to services' onClick={onCancel} />
                    )}
                </Box>
            }
        />
    );
};

ServiceAddHeader.displayName = 'ServiceAddHeader';

ServiceAddHeader.propTypes = {
    onCancel: PropTypes.func,
};

export default ServiceAddHeader;
