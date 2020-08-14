import React from 'react';
import PropTypes from 'prop-types';
import { useParams } from "react-router-dom";

import { Box, Text } from 'grommet';
import { Multiple } from 'grommet-icons';

import AddControl from './Add'

//TODO: navControl, icon
const ServiceEditHeader = ({ compact, role, serviceTypes }) => {
  let { id } = useParams();

  let navControl = <Multiple />;
  let label = <Text size='large' weight={500}>{id}</Text>;
  let icon;

  const showAddControl = !compact && 'read only' !== role;

  return (
    <Box direction="row" align='center' justify="between" fill="horizontal">
      <Box direction="row" gap="small" align='center'>
        {navControl}
        {label}
        {showAddControl && <AddControl serviceTypes={serviceTypes} />}
      </Box>
      {icon}
    </Box>
  );
};

ServiceEditHeader.displayName = 'ServiceEditHeader';

ServiceEditHeader.propTypes = {
  compact: PropTypes.bool,
  role: PropTypes.string
};

export default ServiceEditHeader;
