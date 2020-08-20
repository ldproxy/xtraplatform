import React from 'react';
import PropTypes from 'prop-types';

import { Box, Text } from 'grommet';
import { Multiple, Revert } from 'grommet-icons';

import AddControl from './Add';

// TODO: navControl, icon
const ServiceIndex = ({ compact, role, serviceTypes }) => {
  const navControl = <Multiple />;
  const label = <Text size="large" weight={500}>Services</Text>;
  let icon;

  const showAddControl = !compact && role !== 'read only';

  return (
    <Box direction="row" align="center" justify="between" fill="horizontal">
      <Box direction="row" gap="small" align="center">
        {navControl}
        {label}
        {showAddControl && <AddControl serviceTypes={serviceTypes} />}
      </Box>
      {icon}
    </Box>
  );
};

ServiceIndex.displayName = 'ServiceIndex';

ServiceIndex.propTypes = {
  compact: PropTypes.bool,
  serviceTypes: AddControl.propTypes.serviceTypes,
};

ServiceIndex.defaultProps = {
  compact: false,
};

export default ServiceIndex;
