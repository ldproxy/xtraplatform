import React from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';

import { Box, Text } from 'grommet';
import { Multiple, Menu, Revert, SettingsOption } from 'grommet-icons';
import { Header, IconLink, NavLink, AsyncIcon } from '@xtraplatform/core';

import AddControl from './Add';
import { useView } from '@xtraplatform/manager';

const ServiceIndex = ({ isCompact, role, serviceTypes }) => {
    const showAddControl = !isCompact && role !== 'read only';
    const [{}, { toggleMenu }] = useView();
    const { t } = useTranslation();

    if (isCompact) {
        return (
            <Header
                icon={
                    <IconLink
                        onClick={toggleMenu}
                        icon={<Menu />}
                        title={t('services/ogc_api:manager.menu._label')}
                    />
                }
                label={
                    <NavLink
                        to={{ pathname: '/services' }}
                        label={
                            <Box flex={false} direction='row' gap='xxsmall' align='center'>
                                <Text truncate size='large' weight={500}>
                                    {t('services/ogc_api:services._label')}
                                </Text>
                                <Revert size='list' color='light-5' />
                            </Box>
                        }
                        title={t('services/ogc_api:manager.back._label', {
                            label: t('services/ogc_api:services._label'),
                        })}
                        flex
                    />
                }
            />
        );
    }

    return (
        <Header
            icon={<Multiple />}
            label={t('services/ogc_api:services._label')}
            actions={
                <Box direction='row'>
                    {showAddControl && <AddControl serviceTypes={serviceTypes} />}
                    <NavLink
                        to={{ pathname: '/services/_defaults' }}
                        icon={<SettingsOption />}
                        title={t('services/ogc_api:services.defaults._label')}
                        pad='small'
                    />
                </Box>
            }
        />
    );
};

ServiceIndex.displayName = 'ServiceIndex';

ServiceIndex.propTypes = {
    isCompact: PropTypes.bool,
    serviceTypes: AddControl.propTypes.serviceTypes,
};

ServiceIndex.defaultProps = {
    isCompact: false,
};

export default ServiceIndex;
