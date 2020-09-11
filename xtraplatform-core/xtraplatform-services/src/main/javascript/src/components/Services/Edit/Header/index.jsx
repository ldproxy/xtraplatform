import React from 'react';
import PropTypes from 'prop-types';
import { useFassets } from 'feature-u';

import { Box, Heading } from 'grommet';
import { Globe } from 'grommet-icons';
import { TaskProgress } from '@xtraplatform/core';
import { serviceViewActions } from '../../constants';
import Actions from './Actions';

const ServiceEditHeader = ({ service }) => {
  const ViewActions = useFassets(serviceViewActions());

  const token = null;
  const onSidebarClose = () => { };
  const updateService = () => { };
  const deleteService = () => { };

  return (
    <>
      <Box direction="row" gap="small" align="center">
        <Globe />
        <Heading
          level="3"
          margin="none"
          strong
          truncate
          title={`${service.label} [${service.id}]`}
        >
          {service.label}
        </Heading>
      </Box>
      {service.hasBackgroundTask && <TaskProgress progress={service.progress} message={service.message} />}
      <Actions
        id={service.id}
        status={service.status}
        shouldStart={service.shouldStart}
        secured={service.secured}
        token={token}
        onClose={onSidebarClose}
        updateService={updateService}
        removeService={deleteService}
        ViewActions={ViewActions}
      />
    </>
  );
};

ServiceEditHeader.displayName = 'ServiceEditHeader';

ServiceEditHeader.propTypes = {
  compact: PropTypes.bool,
  role: PropTypes.string,
};

export default ServiceEditHeader;
