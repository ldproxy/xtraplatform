import React from 'react';
import PropTypes from 'prop-types';
import styled from 'styled-components';

import { Box, Text } from 'grommet';
import { Next as NextIcon, Down as DownIcon, Blank as StatusIcon } from 'grommet-icons';
import { ListItem, StyledListItem, StyledListItemSelected } from '../../../List';

const HoverIcon = styled(Box)`
    ${StyledListItem}:hover & svg, ${StyledListItemSelected} & svg {
        stroke: ${(props) => props.theme.global.colors.icon.dark};
        fill: ${(props) => props.theme.global.colors.icon.dark};
    }
    ${StyledListItem}:hover & span, ${StyledListItemSelected} & span {
        color: ${(props) => props.theme.global.colors.icon.dark};
    }
`;

const TreeListItem = ({
    id,
    label,
    icon,
    iconTooltip,
    badge,
    right,
    isFirst,
    isLast,
    isExpandable,
    isExpanded,
    isSelected,
    noExpander,
    depth,
    onExpand,
    onSelect,
}) => {
    return (
        <ListItem
            pad='xsmall'
            hover={true}
            selected={isSelected}
            separator={isFirst ? 'horizontal' : 'bottom'}
            onClick={() => {
                if (!isExpanded) {
                    onExpand(id);
                }
                onSelect(id);
            }}>
            <Box
                direction='row'
                justify='between'
                align='center'
                margin='none'
                fill='horizontal'
                focusIndicator={false}>
                <Box direction='row' align='center' gap='xsmall' margin='none' fill='horizontal'>
                    {depth > 0 &&
                        Array(depth)
                            .fill(0)
                            .map((v, i) => <StatusIcon key={id + i} size='list' />)}
                    <Box
                        title={
                            isExpandable && !noExpander
                                ? isExpanded
                                    ? 'Collapse'
                                    : 'Expand'
                                : iconTooltip
                        }>
                        <HoverIcon>
                            {!isExpandable || noExpander ? (
                                icon || <StatusIcon size='list' />
                            ) : isExpanded ? (
                                <DownIcon
                                    size='list'
                                    onClick={(event) => {
                                        event.stopPropagation();
                                        onExpand(id);
                                    }}
                                />
                            ) : (
                                <NextIcon
                                    size='list'
                                    onClick={(event) => {
                                        event.stopPropagation();
                                        onExpand(id);
                                    }}
                                />
                            )}
                        </HoverIcon>
                    </Box>
                    <Box>
                        <Text size='small'>{label}</Text>
                    </Box>
                    {badge}
                </Box>
                {right}
            </Box>
        </ListItem>
    );
};

TreeListItem.propTypes = {
    id: PropTypes.string.isRequired,
    label: PropTypes.string.isRequired,
    icon: PropTypes.element,
    iconTooltip: PropTypes.string,
    badge: PropTypes.element,
    right: PropTypes.element,
    isFirst: PropTypes.bool,
    isLast: PropTypes.bool,
    isExpandable: PropTypes.bool,
    isExpanded: PropTypes.bool,
    isSelected: PropTypes.bool,
    noExpander: PropTypes.bool,
    depth: PropTypes.number,
    onExpand: PropTypes.func,
    onSelect: PropTypes.func,
};

TreeListItem.defaultProps = {
    icon: null,
    iconTooltip: null,
    badge: null,
    right: null,
    isFirst: false,
    isLast: false,
    isExpandable: false,
    isExpanded: false,
    isSelected: false,
    noExpander: false,
    depth: 0,
    onExpand: () => {},
    onSelect: () => {},
};

TreeListItem.displayName = 'TreeListItem';

export default TreeListItem;
