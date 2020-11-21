import React, { useCallback, useState } from 'react';
import PropTypes from 'prop-types';

import { List } from '../../List';
import Item from './Item';

const getItemsForTree = (tree, parent, expanded, showRoot = true, expandAll = true) => {
    const leafList = [];
    const roots = tree && tree.length ? tree.filter((leaf) => leaf.parent === parent) : [];
    roots.forEach((root) => {
        const leafs = getItemsForLeaf(tree, root, expanded, showRoot, expandAll);
        leafList.push(...leafs);
    });

    return leafList;
};

const getItemsForLeaf = (tree, leaf, expanded, showRoot, expandAll, depth = -1) => {
    const leafList = [];
    const children = tree.filter((child) => child.parent === leaf.id);
    const isExpanded = expandAll || expanded.indexOf(leaf.id) > -1;

    if (depth === -1 && showRoot) depth = 0;

    if (leaf.parent || showRoot) {
        leafList.push({
            ...leaf,
            depth,
            isExpanded,
            isExpandable: children.length > 0,
        });
    }

    if (isExpanded) {
        children
            .map((child) => getItemsForLeaf(tree, child, expanded, showRoot, expandAll, depth + 1))
            .forEach((childLeafList) => leafList.push(...childLeafList));
    }

    return leafList;
};

const TreeList = ({
    tree,
    expanded,
    selected,
    parent,
    hideRoot,
    noRootExpander,
    noExpanders,
    onSelect,
}) => {
    const [currentExpanded, setExpanded] = useState(expanded);
    const [currentSelected, setSelected] = useState(selected);

    const onExpand = useCallback(
        (id) => {
            const newExpanded =
                currentExpanded.indexOf(id) > -1
                    ? currentExpanded.filter((i) => i !== id)
                    : currentExpanded.concat(id);
            setExpanded(newExpanded);
        },
        [currentExpanded, setExpanded]
    );

    const onSelectWrapper = useCallback(
        (id) => {
            setSelected(id);
            onSelect(id);
        },
        [setSelected, onSelect]
    );

    const items = getItemsForTree(tree, parent, currentExpanded, !hideRoot, noExpanders);

    return (
        <List>
            {items.map((leaf, i) => (
                <Item
                    {...leaf}
                    key={`${leaf.parent}_${leaf.id}`}
                    isFirst={i === 0}
                    isLast={i === items.length - 1}
                    isSelected={currentSelected === leaf.id}
                    noExpander={(i === 0 && noRootExpander) || noExpanders}
                    onExpand={onExpand}
                    onSelect={onSelectWrapper}
                />
            ))}
        </List>
    );
};

TreeList.propTypes = {
    tree: PropTypes.array.isRequired,
    expanded: PropTypes.array,
    selected: PropTypes.string,
    onSelect: PropTypes.func,
    hideRoot: PropTypes.bool,
    noRootExpander: PropTypes.bool,
    noExpanders: PropTypes.bool,
    parent: PropTypes.string,
};

TreeList.defaultProps = {
    expanded: [],
    selected: null,
    onSelect: () => {},
    hideRoot: false,
    noRootExpander: false,
    noExpanders: false,
    parent: null,
};

TreeList.displayName = 'TreeList';

export default TreeList;
