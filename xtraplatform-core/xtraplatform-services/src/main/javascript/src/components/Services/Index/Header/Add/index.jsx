import React from 'react';
import PropTypes from 'prop-types';
import { useHistory } from 'react-router-dom';
import { useTranslation } from 'react-i18next';

import { Menu } from 'grommet';
import { Add } from 'grommet-icons';
import { NavLink } from '@xtraplatform/core';

const ServiceIndexHeaderAdd = ({ serviceTypes }) => {
    if (!serviceTypes || serviceTypes.length === 0) {
        return null;
    }

    // TODO: define somewhere else
    const route = '/services/_add';
    const history = useHistory();

    const { t } = useTranslation();

    return serviceTypes.length === 1 ? (
        <NavLink
            icon={<Add />}
            to={`${route}`} //?type=${serviceTypes[0].id}
            title={t('services/ogc_api:services.add._label')}
            pad='small'
        />
    ) : (
        <Menu
            icon={<Add />}
            title={t('services/ogc_api:services.add._label')}
            items={serviceTypes.map((type) => ({
                label: type.label || type.id,
                title: `Add ${type.label || type.id} service`,
                onClick: () => history.push(`${route}?type=${type.id}`),
            }))}
        />
    );
};

ServiceIndexHeaderAdd.displayName = 'ServiceIndexHeaderAdd';

ServiceIndexHeaderAdd.propTypes = {
    serviceTypes: PropTypes.arrayOf(
        PropTypes.shape({
            id: PropTypes.string.isRequired,
            label: PropTypes.string,
        })
    ),
};

export default ServiceIndexHeaderAdd;
