import React from 'react';
import PropTypes from 'prop-types';

import { Box, Image, Text, Button } from 'grommet';
import { Close } from 'grommet-icons'

const NavigationHeader = ({ isLayer, onClose, title, logo }) => {
    return (
        <Box fill="horizontal" flex={false} pad={{ right: 'small' }}>
            <Box
                direction="row"
                fill="horizontal"
                height="xsmall"
                gap="small"
                justify="between"
                align="center"
                alignContent="center"
                flex={false}
            >
                <Box pad={{ left: 'medium' }}>
                    {logo
                        ? <Image fit="contain" alignSelf="start" src={logo} />
                        : <Text size="large" weight={500}>{title}</Text>}
                </Box>
                {isLayer && (
                    <Button
                        icon={<Close size="medium" />}
                        onClick={onClose}
                        plain
                        a11yTitle="Close Menu"
                    />
                )}
            </Box>
        </Box>
    );
};

NavigationHeader.displayName = 'NavigationHeader';

NavigationHeader.propTypes = {
    isLayer: PropTypes.bool,
    onClose: PropTypes.func,
    title: PropTypes.string,
    logo: PropTypes.string,
};

export default NavigationHeader;
