import React, { Component } from 'react';
import PropTypes from 'prop-types';

import Tile from 'grommet/components/Tile';
import Card from 'grommet/components/Card';
import Heading from 'grommet/components/Heading';
import StatusIcon from 'grommet/components/icons/Status';
import Spinning from 'grommet/components/icons/Spinning';
import { Link } from 'redux-little-router';

export default class ServiceTile extends Component {

    render() {
        const {item, changeLocation, selected} = this.props;

        let status = item.status === 'INITIALIZING' ? 'Initializing' : (item.status === 'STARTED' ? 'Online' : 'Offline');
        let icon = item.status === 'INITIALIZING' ? <Spinning size="medium" style={ { verticalAlign: 'middle', marginRight: '6px' } } /> : <StatusIcon value={ item.status === 'STARTED' ? 'ok' : 'critical' } size="medium" />
        return (
            <Tile align="start"
                pad="small"
                direction="column"
                size="large"
                onClick={ () => changeLocation(`/services/${item.id}`) }
                selected={ selected }
                a11yTitle={ `View ${item.name} Virtual Machine` }
                colorIndex="light-1"
                separator="all"
                hoverStyle="border"
                hoverColorIndex="accent-1"
                hoverBorderSize="large">
                <Card heading={ <Heading tag="h3" strong={ true }>
                                    { item.name }
                                </Heading> }
                    textSize="small"
                    label={ item.id }
                    description={ <span>{ icon } <span style={ { verticalAlign: 'middle' } }>{ status }</span></span> } />
            </Tile>
        );
    }
}

ServiceTile.propTypes = {
    item: PropTypes.object.isRequired,
    changeLocation: PropTypes.func,
    selected: PropTypes.bool
};

ServiceTile.defaultProps = {
};
