import React from 'react';
import PropTypes from 'prop-types';

import { Box, Text } from 'grommet';
import { Multiple, Menu, Revert, SettingsOption } from 'grommet-icons';
import { Header, IconLink, NavLink, AsyncIcon } from '@xtraplatform/core';

import AddControl from './Add';
import { useView } from '@xtraplatform/manager';

const ServiceIndex = ({ isCompact, role, serviceTypes }) => {
    const showAddControl = !isCompact && role !== 'read only';
    const [{}, { toggleMenu }] = useView();

    if (isCompact) {
        return (
            <Header
                icon={<IconLink onClick={toggleMenu} icon={<Menu />} title='Show menu' />}
                label={
                    <NavLink
                        to={{ pathname: '/services' }}
                        label={
                            <Box flex={false} direction='row' gap='xxsmall' align='center'>
                                <Text truncate size='large' weight={500}>
                                    Services
                                </Text>
                                <Revert size='list' color='light-5' />
                            </Box>
                        }
                        title='Go back to services'
                        flex
                    />
                }
            />
        );
    }

    return (
        <Header
            icon={<Multiple />}
            label='Services'
            actions={
                <Box direction='row'>
                    {showAddControl && <AddControl serviceTypes={serviceTypes} />}
                    <NavLink
                        to={{ pathname: '/services/_defaults' }}
                        icon={<SettingsOption />}
                        title='Edit service defaults'
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
