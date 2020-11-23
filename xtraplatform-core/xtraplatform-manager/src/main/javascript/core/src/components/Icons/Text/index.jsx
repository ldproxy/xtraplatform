import React from 'react';
import PropTypes from 'prop-types';
import styled from 'styled-components';

import { Box, Text } from 'grommet';

const StyledBox = styled(Box)`
    width: ${(props) => props.theme.icon.size[props.size]};
    height: ${(props) => props.theme.icon.size[props.size]};
    font-family: 'Roboto Mono', monospace;
`;

const TextIcon = ({ text, size, color }) => {
    return (
        <StyledBox justify='center' align='center' size={size}>
            <Text size={size} color={color} weight='bold'>
                {text}
            </Text>
        </StyledBox>
    );
};

TextIcon.propTypes = {
    text: PropTypes.string.isRequired,
    size: PropTypes.oneOf(['small', 'list', 'medium', 'large']),
    color: PropTypes.string,
};

TextIcon.defaultProps = {
    size: 'medium',
    color: 'icon',
};

TextIcon.displayName = 'TextIcon';

export default TextIcon;
