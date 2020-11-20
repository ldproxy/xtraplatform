import React from 'react';
import PropTypes from 'prop-types';

import ViewContext from '../Context';
import { useProvideView } from '../../../../hooks/view';

// Provider component that wraps your app and makes auth object ...
// ... available to any child component that calls useAuth().
const ViewProvider = ({ isAdvanced, children }) => {
    const auth = useProvideView(isAdvanced);

    return <ViewContext.Provider value={auth}>{children}</ViewContext.Provider>;
};

ViewProvider.displayName = 'ViewProvider';

ViewProvider.propTypes = {
    isAdvanced: PropTypes.bool,
};

ViewProvider.defaultProps = {
    isAdvanced: false,
};

export default ViewProvider;
