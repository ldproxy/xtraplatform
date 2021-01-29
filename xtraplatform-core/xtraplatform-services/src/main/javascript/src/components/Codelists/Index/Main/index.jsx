import React from 'react';
import PropTypes from 'prop-types';

import { Box, ResponsiveContext } from 'grommet';
import { List, ListItem } from '@xtraplatform/core';

const CodelistIndexMain = ({ isCompact, codelists, onSelect }) => {
    return (
        <ResponsiveContext.Consumer>
            {(size) => {
                const isSmall = isCompact || size === 'small';

                return (
                    <Box
                        pad={{ horizontal: 'small', vertical: 'medium' }}
                        fill={true}
                        overflow={{ vertical: 'auto', horizontal: 'hidden' }}>
                        <List compact={isSmall}>
                            {codelists.map((codelist, i) => (
                                <ListItem
                                    {...codelist}
                                    key={codelist.id}
                                    separator={i === 0 ? 'horizontal' : 'bottom'}
                                    hover={true}
                                    onClick={(e) => {
                                        e.target.blur();
                                        onSelect(codelist.id);
                                    }}>
                                    {codelist.id}
                                </ListItem>
                            ))}
                        </List>
                    </Box>
                );
            }}
        </ResponsiveContext.Consumer>
    );
};

CodelistIndexMain.displayName = 'CodelistIndexMain';

CodelistIndexMain.propTypes = {
    isCompact: PropTypes.bool,
    codelists: PropTypes.arrayOf(
        PropTypes.shape({
            id: PropTypes.string.isRequired,
        })
    ).isRequired,
};

CodelistIndexMain.defaultProps = {
    isCompact: false,
};

export default CodelistIndexMain;
