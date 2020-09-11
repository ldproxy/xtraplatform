import React from 'react';
import PropTypes from 'prop-types';
import { useParams } from 'react-router-dom';

import { Box, ResponsiveContext } from 'grommet';
import { TileGrid } from '@xtraplatform/core';
import Tile from './Tile';

// TODO: messages
const ServiceIndexMain = ({ isCompact, services }) => {
  const { id } = useParams();

  return (
    <ResponsiveContext.Consumer>
      {(size) => {
        const isSmall = isCompact || size === 'small';

        return (
          <Box fill="vertical" overflow={{ vertical: 'auto' }}>
            <Box pad="none" background="content" flex={false}>
              <TileGrid compact={isSmall}>
                {services.map((service) => <Tile {...service} key={service.id} isCompact={isSmall} isSelected={service.id === id} />)}
              </TileGrid>
            </Box>
          </Box>
        );
      }}
    </ResponsiveContext.Consumer>
  );
};

ServiceIndexMain.displayName = 'ServiceIndexMain';

ServiceIndexMain.propTypes = {
  isCompact: PropTypes.bool,
  services: PropTypes.arrayOf(
    PropTypes.shape({
      id: PropTypes.string.isRequired,
    }),
  ).isRequired,
};

ServiceIndexMain.defaultProps = {
  isCompact: false,
};

export default ServiceIndexMain;
