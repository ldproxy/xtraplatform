import React from 'react';
import PropTypes from 'prop-types';

import {
    StatusCritical,
    StatusDisabled,
    StatusGood,
    StatusUnknown,
    StatusWarning,
    CircleAlert,
} from 'grommet-icons';

const VALUE_ICON = {
    unknown: StatusUnknown,
    ok: StatusGood,
    warning: StatusWarning,
    critical: CircleAlert,
    disabled: StatusDisabled,
};

const IconsStatus = ({ value, ...rest }) => {
    const Icon = VALUE_ICON[value.toLowerCase()] || StatusUnknown;

    return <Icon color={`status-${value.toLowerCase()}`} {...rest} />;
};

IconsStatus.displayName = 'IconsStatus';

IconsStatus.propTypes = {
    /**
     * The status code
     */
    value: PropTypes.oneOf(['unknown', 'ok', 'warning', 'critical', 'disabled']).isRequired,
};

IconsStatus.defaultProps = {};

export default IconsStatus;
