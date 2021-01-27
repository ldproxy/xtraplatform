import React from 'react';
import styled from 'styled-components';

import { Box } from 'grommet';

export const StyledListItem = styled(Box)`
    background-color: ${(props) => props.background};
    color: ${(props) => props.theme.global.colors.text.light};
    cursor: ${(props) => (props.onClick ? 'pointer' : 'default')};

    &:hover {
        background-color: ${(props) =>
            props.theme.normalizeColor(props.theme.list.hover.background)};
        color: ${(props) => props.theme.global.colors.text.dark};
    }
`;

export const StyledListItemSelected = styled(Box)`
    background-color: ${(props) =>
        props.theme.normalizeColor(props.theme.list.selected.background)};
    color: ${(props) => props.theme.global.colors.text.dark};
`;

export const List = (props) => (
    <Box
        fill
        as='ul'
        pad='none'
        margin='none'
        border={{ side: 'top', color: 'light-4' }}
        {...props}
    />
);

export const ListItem = (props) => {
    const Li = props.hover ? (props.selected ? StyledListItemSelected : StyledListItem) : Box;
    return (
        <Li
            tag='li'
            border={{ side: 'bottom', color: 'light-4' }}
            pad='small'
            direction='row'
            justify='between'
            flex={false}
            focusIndicator={false}
            {...props}
        />
    );
};
