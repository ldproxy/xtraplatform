import React from 'react';
import PropTypes from 'prop-types';

import { Box } from 'grommet';
import { List, ListItem } from '@xtraplatform/core';

const CodelistDetailsMain = ({ entries }) => {
    return (
        <Box
            pad={{ horizontal: 'small', vertical: 'medium' }}
            fill={true}
            overflow={{ vertical: 'auto', horizontal: 'hidden' }}>
            <List>
                {Object.keys(entries).map((key, i) => (
                    <ListItem key={key} separator={i === 0 ? 'horizontal' : 'bottom'} hover={true}>
                        <Box direction='row' fill='horizontal' justify='between'>
                            <span>{key}</span>
                            <span>{entries[key]}</span>
                        </Box>
                    </ListItem>
                ))}
            </List>
        </Box>
    );
};

CodelistDetailsMain.displayName = 'CodelistDetailsMain';

CodelistDetailsMain.propTypes = {};

export default CodelistDetailsMain;
