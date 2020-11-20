import React from 'react';
import PropTypes from 'prop-types';

import { Box, Heading } from 'grommet';

const Header = ({ icon, label, title, actions, actions2 }) => {
    return (
        <Box direction='row' gap='small' align='center' justify='between' fill='horizontal'>
            <Box direction='row' gap='small' align='center'>
                {icon}
                {typeof label === 'string' ? (
                    <Heading level='3' size='medium' margin='none' truncate title={title}>
                        {label}
                    </Heading>
                ) : (
                    label
                )}
                {actions2}
            </Box>
            {actions}
        </Box>
    );
};

Header.displayName = 'Header';

Header.propTypes = {
    icon: PropTypes.element.isRequired,
    label: PropTypes.oneOfType([PropTypes.element, PropTypes.string]).isRequired,
    title: PropTypes.string,
    actions: PropTypes.element,
    actions2: PropTypes.element,
};

Header.defaultProps = {
    title: null,
    actions: null,
    actions2: null,
};

export default Header;
