import React, { Component, PropTypes } from 'react';

class TreeList extends Component {

    constructor(props) {
        super(props);

        this._initState();
    }

    componentWillReceiveProps(nextProps) {
        this._initState(nextProps);
    }

    _initState = (nextProps) => {
        if (this.state && nextProps) {
            const {expanded} = this.props;
            var expandedChanged = nextProps.expanded && ((nextProps.expanded.length != expanded.length) || nextProps.expanded.every(function(element, index) {
                    return element !== expanded[index];
                }));
            var selectedChanged = /*nextProps.selected &&*/ nextProps.selected !== this.props.selected

            var newState = {}
            if (expandedChanged)
                newState.expanded = this.state.expanded.concat(nextProps.expanded);
            if (selectedChanged)
                newState.selected = nextProps.selected
            if (expandedChanged || selectedChanged)
                this.setState(newState);
        } else if (!this.state) {
            this.state = {
                selected: this.props.selected || null,
                expanded: this.props.expanded || []
            }
        }
    }

    _expand = (leaf) => {
        const {expanded} = useInternalState ? this.state : this.props;
        const {onExpand} = this.props;

        const newExpanded = expanded.indexOf(leaf._id) > -1 ? expanded.filter(i => i !== leaf._id) : expanded.concat(leaf._id)

        if (useInternalState) {
            this.setState({
                expanded: newExpanded
            });
        }

        if (onExpand) {
            onExpand(leaf);
        }
    };

    _select = (leaf) => {
        const {tree, onSelect} = this.props;

        this.setState({
            selected: leaf._id
        });

        onSelect(leaf._id, leaf);
    };

    _renderLeafs = (root, depth = 0, packageList = []) => {
        const {tree, renderLeaf, doRenderRoot} = this.props;
        const {expanded, selected} = this.state;

        if (root) {
            let children = tree.filter(leaf => leaf.parent === root._id)

            if (doRenderRoot || root.parent) {
                const isFirst = packageList.length === 0;
                const isLast = packageList.length === tree.length - 2;
                const isSelected = root._id === selected;
                const isExpanded = expanded.indexOf(root._id) > -1;
                const hasChildren = children && children.length > 0;
                const onSelect = this._select;
                const onExpand = this._expand;

                packageList.push(renderLeaf(root, isFirst, isLast, isSelected, isExpanded, hasChildren, depth, onSelect, onExpand))
            }

            if (expanded.indexOf(root._id) > -1) {
                children.forEach(leaf => this._renderLeafs(leaf, depth + 1, packageList))
            }
        }

        return packageList
    }



    render() {
        const {tree, renderTree} = this.props;
        const {expanded, selected} = this.state;

        const treeRoot = tree && tree.length ? tree.find(leaf => leaf.parent === null) : null;
        const leafList = treeRoot ? this._renderLeafs(treeRoot) : null;

        return leafList && renderTree(leafList, selected, expanded, this._select, this._expand)
    }
}

TreeList.propTypes = {
    tree: PropTypes.array,
    onSelect: PropTypes.func,
    onExpand: PropTypes.func,
    selected: PropTypes.string,
    expanded: PropTypes.array,
    renderTree: PropTypes.func,
    renderLeaf: PropTypes.func,
    useInternalState: PropTypes.bool,
    doRenderRoot: PropTypes.bool
};

TreeList.defaultProps = {
    onSelect: () => {
    },
    onExpand: () => {
    },
    useInternalState: true
};

export default TreeList;
