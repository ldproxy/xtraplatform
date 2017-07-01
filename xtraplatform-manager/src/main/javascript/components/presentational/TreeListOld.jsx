import React, { Component, PropTypes } from 'react';

import Box from 'grommet/components/Box';
import List from 'grommet/components/List';
import ListItem from 'grommet/components/ListItem';
import AddIcon from 'grommet/components/icons/base/Add';
import MinusIcon from 'grommet/components/icons/base/Subtract';
import StatusIcon from 'grommet/components/icons/Status';

class TreeList extends Component {

    constructor(props) {
        super(props);

        const {tree, selected} = this.props;

        this.state = {
            selected: selected || 0,
            expanded: tree.map((leaf, i) => i).filter((leaf, i) => tree[i].expanded)
        }
    }

    componentWillReceiveProps(nextProps) {
        /*const {tree, selected} = nextProps
        if (tree) {
            console.log('componentWillReceiveProps', tree, selected);
            this.setState({
                selected: selected || 0,
                expanded: tree.map((leaf, i) => i).filter((leaf, i) => tree[i].expanded)
            })
        }*/
    }

    _expand = (toggled) => {
        const {tree, onExpand} = this.props;
        const {expanded} = this.state;

        const newExpanded = toggled in expanded ? expanded.filter(i => i !== toggled) : expanded.concat(toggled)

        this.setState({
            expanded: newExpanded
        })

        onExpand(tree[toggled].id);
    };

    _select = (selected) => {
        const {tree, onSelect} = this.props;

        this.setState({
            selected: selected
        });

        onSelect(tree[selected].id);
    };

    render() {
        const {tree} = this.props;
        const {selected, expanded} = this.state;

        return (
            <List selectable={ true } selected={ selected } onSelect={ this._select }>
                { tree.filter(leaf => leaf.parent === -1 || leaf.parent in expanded)
                      .map((leaf, i) => <ListItem key={ leaf.id } separator={ i === 0 ? 'horizontal' : 'bottom' }>
                                            <Box direction="row" pad={ { between: 'small' } }>
                                                { !leaf.expandable ?
                                                  <span><StatusIcon value="blank" size="medium" /> <StatusIcon value="unknown" size="small" /></span>
                                                  :
                                                  i in expanded ?
                                                  <MinusIcon size="small" onClick={ () => this._expand(i) } />
                                                  :
                                                  <AddIcon size="small" onClick={ () => this._expand(i) } /> }
                                                <span className="message">{ leaf.title }</span>
                                            </Box>
                                        </ListItem>
                  ) }
            </List>
        );
    }
}

TreeList.propTypes = {
    tree: PropTypes.array.isRequired,
    onSelect: PropTypes.func,
    onExpand: PropTypes.func
};

TreeList.defaultProps = {
    onSelect: () => {
    },
    onExpand: () => {
    }
};

export default TreeList;
//
///*<Collapsible key={ key } active={ this.state.expanded[featureType.id] }>*/
////*</Collapsible>*/