import React from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';

import { Box, Text } from 'grommet';
import { Grid, Menu, Revert } from 'grommet-icons';
import { Header, IconLink, NavLink } from '@xtraplatform/core';

import { useView } from '@xtraplatform/manager';

const ServiceIndexHeader = ({ isCompact }) => {
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
                        to={{ pathname: '/codelists' }}
                        label={
                            <Box flex={false} direction='row' gap='xxsmall' align='center'>
                                <Text truncate size='large' weight={500}>
                                    {t('services/ogc_api:codelists._label')}
                                </Text>
                                <Revert size='list' color='light-5' />
                            </Box>
                        }
                        title={t('services/ogc_api:manager.back._label', {
                            label: t('services/ogc_api:codelists._label'),
                        })}
                        flex
                    />
                }
            />
        );
    }

    return (
        <Header
            icon={<Grid />}
            label={t('services/ogc_api:codelists._label')}
            actions={<Box direction='row'></Box>}
        />
    );
};

ServiceIndexHeader.displayName = 'ServiceIndexHeader';

ServiceIndexHeader.propTypes = {
    isCompact: PropTypes.bool,
};

ServiceIndexHeader.defaultProps = {
    isCompact: false,
};

export default ServiceIndexHeader;
