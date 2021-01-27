import React from 'react';
import PropTypes from 'prop-types';

import { Button, Box } from 'grommet';
import styled from 'styled-components';

const SelectableBox = styled(Box)`
    ${(props) => props.selected && `border-color: ${props.theme.normalizeColor('control')};`}

    &:hover {
        border-color: ${(props) => props.theme.normalizeColor('control')};
    }
`;

const Tile = ({ onClick, basis, background, fill, pad, ...rest }) => {
    return (
        <Box
            fill={fill}
            basis={basis}
            flex='grow'
            margin={{ horizontal: 'xsmall', vertical: 'xsmall' }}>
            <Button plain focusIndicator={false} onClick={onClick}>
                <SelectableBox
                    pad={pad || 'small'}
                    border={{ side: 'all', color: 'light-4', size: 'small' }}
                    background={background}
                    {...rest}
                />
            </Button>
        </Box>
    );
};

Tile.displayName = 'Tile';

Tile.propTypes = {
    onClick: PropTypes.func,
    basis: PropTypes.string,
    background: PropTypes.string,
    fill: PropTypes.string,
    pad: PropTypes.oneOfType([PropTypes.string, PropTypes.object]),
};

Tile.defaultProps = {
    onClick: null,
    basis: null,
    background: null,
    fill: null,
    pad: null,
};

export default Tile;
