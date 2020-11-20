import React from 'react';

import { Box } from 'grommet';
import styled from 'styled-components';

const StyledBox = styled(Box)`
    background-color: ${(props) =>
        props.selected ? props.theme.global.colors.active : props.background};
    color: ${(props) =>
        props.selected
            ? props.theme.global.colors.text.dark
            : props.theme.global.colors.text.light};
    cursor: ${(props) => (props.onClick ? 'pointer' : 'default')};

    &:hover {
        background-color: ${(props) =>
            props.selected ? props.theme.global.colors.active : props.theme.global.colors.hover};
        color: ${(props) => props.theme.global.colors.text.dark};

        svg {
            stroke: ${(props) => props.theme.global.colors.icon.dark};
            fill: ${(props) => props.theme.global.colors.icon.dark};
        }
    }

    & svg {
        stroke: ${(props) =>
            props.selected
                ? props.theme.global.colors.icon.dark
                : props.theme.global.colors.icon.light};
        fill: ${(props) =>
            props.selected
                ? props.theme.global.colors.icon.dark
                : props.theme.global.colors.icon.light};
    }
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
    const Li = props.hover ? StyledBox : Box;
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
