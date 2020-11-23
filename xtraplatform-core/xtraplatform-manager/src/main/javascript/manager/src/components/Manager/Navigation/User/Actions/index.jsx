import React from 'react';
import PropTypes from 'prop-types';

import { Box, Text, DropButton, Button, Menu } from 'grommet';
import { User } from 'grommet-icons';

const UserActions = ({ name, onLogout, onChangePassword }) => {
    const items = [
        {
            label: name,
            color: 'light-1',
            disabled: true,
        },
    ];
    return (
        <Menu
            icon={<User color='light-1' />}
            margin={{ vertical: 'medium', horizontal: 'small' }}
            dropProps={{ align: { bottom: 'bottom', left: 'left' }, plain: true }}
            dropBackground='navigationOverlay'
            items={items}
        />
        /*<Box pad={{ vertical: 'medium', horizontal: 'small' }}>
            <DropButton
                icon={<User color='light-1' />}
                dropAlign={{ bottom: 'top', left: 'left' }}
                dropContent={
                    <Box pad='small' gap='small'>
                        <Box
                            border={{ side: 'bottom', size: 'small' }}
                            pad={{ bottom: 'small' }}
                            align='center'
                            flex={false}>
                            <Text weight='bold'>{name}</Text>
                        </Box>
                        <Box flex={false}>
                            {onChangePassword && (
                                <Box>
                                    <Button
                                        onClick={onChangePassword}
                                        plain={true}
                                        fill='horizontal'
                                        hoverIndicator={true}>
                                        <Box pad={{ vertical: 'xsmall' }} align='center'>
                                            Change password
                                        </Box>
                                    </Button>
                                </Box>
                            )}
                            {onLogout && (
                                <Box>
                                    <Button
                                        onClick={onLogout}
                                        plain={true}
                                        fill='horizontal'
                                        hoverIndicator={true}>
                                        <Box pad={{ vertical: 'xsmall' }} align='center'>
                                            Logout
                                        </Box>
                                    </Button>
                                </Box>
                            )}
                        </Box>
                    </Box>
                }></DropButton>
            </Box>*/
    );
};

UserActions.displayName = 'UserActions';

export default UserActions;
