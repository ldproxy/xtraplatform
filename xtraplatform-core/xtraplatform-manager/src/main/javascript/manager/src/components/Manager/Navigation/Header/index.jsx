import React from 'react';
import PropTypes from 'prop-types';

import { Box, Image, Text, Button } from 'grommet';
import { Close } from 'grommet-icons';

const NavigationHeader = ({ isLayer, title, logo, color, onClose }) => {
    return (
        <Box fill='horizontal' flex={false} pad={{ right: 'small' }}>
            <Box
                direction='row'
                fill='horizontal'
                height='xsmall'
                gap='large'
                justify='between'
                align='center'
                alignContent='center'
                flex={false}>
                <Box pad={{ left: 'medium' }}>
                    {logo ? (
                        <Image fit='contain' alignSelf='start' src={logo} />
                    ) : (
                        <Text size='large' weight={500} color={color}>
                            {title}
                        </Text>
                    )}
                </Box>
                {isLayer && (
                    <Button
                        icon={<Close size='medium' />}
                        onClick={onClose}
                        plain
                        a11yTitle='Close Menu'
                    />
                )}
            </Box>
        </Box>
    );
};

NavigationHeader.displayName = 'NavigationHeader';

NavigationHeader.propTypes = {
    isLayer: PropTypes.bool,
    title: PropTypes.string,
    logo: PropTypes.string,
    onClose: PropTypes.func,
};

NavigationHeader.defaultProps = {
    isLayer: false,
    title: null,
    logo: null,
    onClose: null,
};

export default NavigationHeader;
