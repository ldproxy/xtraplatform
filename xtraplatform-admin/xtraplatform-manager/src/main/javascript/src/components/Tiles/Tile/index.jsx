import React from 'react';
import PropTypes from 'prop-types';

import { Button, Box } from 'grommet';
import styled from "styled-components";


const SelectableBox = styled(Box)`
${props => props.selected && `border-color: ${props.theme.global.colors.active};`}

&:hover {
        border-color: ${props => props.theme.global.colors.active};
    }
`;

const Tile = ({ onClick, basis, background, fill, pad, ...rest }) => {

  const onClickBlur = event => {
    event.currentTarget.blur();
    onClick();
  }

  return (
    <Box fill={fill} basis={basis} flex="grow" margin={{ horizontal: 'xsmall', vertical: 'xsmall' }}>
      <Button plain={true} onClick={onClickBlur}>
        <SelectableBox pad={pad || 'small'} border={{ side: 'all', color: 'light-4', size: 'small' }} background={background} {...rest} />
      </Button>
    </Box>
  );
};

Tile.displayName = 'Tile';

Tile.propTypes = {
  onClick: PropTypes.func
};

export default Tile;
