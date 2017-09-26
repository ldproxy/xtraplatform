import React, { Component } from 'react';
import PropTypes from 'prop-types';

class TreeList extends Component {

    _renderLeafs = (leaf, depth = -1, leafList = []) => {
        const {tree, renderLeaf, expanded, selected, onSelect, onExpand, doRenderRoot} = this.props;

        if (depth === -1 && doRenderRoot)
            depth = 0

        if (leaf) {
            let children = tree.filter(child => child.parent === leaf._id)

            if (leaf.parent || doRenderRoot) {
                const isFirst = leafList.length === 0;
                const isLast = leafList.length === tree.length - 2;
                const isSelected = leaf._id === selected;
                const isExpanded = expanded.indexOf(leaf._id) > -1;
                const hasChildren = children && children.length > 0;

                leafList.push(renderLeaf(leaf, isFirst, isLast, isSelected, isExpanded, hasChildren, depth, onSelect, onExpand))
            }

            if (expanded.indexOf(leaf._id) > -1) {
                children.forEach(child => this._renderLeafs(child, depth + 1, leafList))
            }
        }

        return leafList
    }



    render() {
        const {tree, parent, renderTree, expanded, selected, onSelect, onExpand} = this.props;

        const treeRoot = tree && tree.length ? tree.filter(leaf => leaf.parent === parent) : null;
        let leafList = [];

        if (treeRoot) {
            treeRoot.forEach(leaf => {
                leafList = leafList.concat(this._renderLeafs(leaf));
            })
        }

        return renderTree(leafList, selected, expanded, onSelect, onExpand)
    }
}

TreeList.propTypes = {
    tree: PropTypes.array,
    onSelect: PropTypes.func,
    onExpand: PropTypes.func,
    selected: PropTypes.string,
    expanded: PropTypes.array,
    renderTree: PropTypes.func.isRequired,
    renderLeaf: PropTypes.func.isRequired,
    doRenderRoot: PropTypes.bool,
    parent: PropTypes.string
};

TreeList.defaultProps = {
    onSelect: () => {
    },
    onExpand: () => {
    },
    parent: null
};

export default TreeList;
