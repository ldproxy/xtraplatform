import React, { Component, PropTypes } from 'react';

import Box from 'grommet/components/Box';
import List from 'grommet/components/List';
import ListItem from 'grommet/components/ListItem';
import AddIcon from 'grommet/components/icons/base/Add';
import MinusIcon from 'grommet/components/icons/base/Subtract';
import NextIcon from 'grommet/components/icons/base/Next';
import DownIcon from 'grommet/components/icons/base/Down';
import StopIcon from 'grommet/components/icons/base/Stop';
import StatusIcon from 'grommet/components/icons/Status';

import TreeList from './TreeList'

class GrommetTreeList extends Component {

    _select = (onSelect, index) => {
        const {tree} = this.props;

        onSelect(tree[index])
    }

    _expand = (onExpand, index) => {
        const {tree} = this.props;

        onExpand(tree[index])
    }

    _renderTree = (leafList, selected, expanded, onSelect, onExpand) => {
        const {tree} = this.props;

        //this._select(onSelect, 0);

        return <List selectable={ true } selected={ tree.findIndex(leaf => leaf._id === selected) } /*onSelect={ this._select.bind(this, onSelect) }*/>
               { leafList }
               </List>
    }

    _renderLeaf = (leaf, isFirst, isLast, isSelected, isExpanded, hasChildren, depth, onSelect, onExpand) => {

        return <ListItem key={ leaf._id } separator={ isFirst ? 'horizontal' : 'bottom' } onClick={ () => onSelect(leaf) }>
                   <Box direction="row" pad={ { between: 'small' } }>
                       <span>{ depth > 0 && Array(depth).fill(0).map((v, i) => <StatusIcon key={ i } value="blank" size="medium" />) }</span>
                       { !leaf.expandable ?
                         <StopIcon size="small" />
                         :
                         isExpanded ?
                         <MinusIcon size="small" /*onClick={ ()=> onExpand(leaf) }*/ />
                         :
                         <AddIcon size="small" /*onClick={ ()=> onExpand(leaf) }*/ /> }
                       <span className="message">{ leaf.title }</span>
                   </Box>
               </ListItem>
    }

    render() {
        return <TreeList renderTree={ this._renderTree }
                   renderLeaf={ this._renderLeaf }
                   doRenderRoot={ true }
                   {...this.props}/>
    }
}

GrommetTreeList.propTypes = {
};

GrommetTreeList.defaultProps = {
};

export default GrommetTreeList;
