import React from 'react';
import PropTypes from 'prop-types';

import { StatusCritical, StatusDisabled, StatusGood, StatusUnknown, StatusWarning } from 'grommet-icons';

const VALUE_ICON = {
  critical: StatusCritical,
  disabled: StatusDisabled,
  ok: StatusGood,
  unknown: StatusUnknown,
  warning: StatusWarning,
};

const IconsStatus = ({ value, ...rest }) => {
  const Icon = VALUE_ICON[value.toLowerCase()] || StatusUnknown;

  return <Icon color={`status-${value.toLowerCase()}`} {...rest} />;
};

IconsStatus.displayName = 'IconsStatus';

IconsStatus.propTypes = {
  value: PropTypes.string.isRequired
};

IconsStatus.defaultProps = {
};

export default IconsStatus;
