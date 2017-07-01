import React, { Component, PropTypes } from 'react';
import Tile from 'grommet/components/Tile';
import Card from 'grommet/components/Card';
import Map from 'grommet/components/Map';
import Box from 'grommet/components/Box';
import Button from 'grommet/components/Button';
import Heading from 'grommet/components/Heading';
import StatusIcon from 'grommet/components/icons/Status';
import Spinning from 'grommet/components/icons/Spinning';
import LinkNext from 'grommet/components/icons/base/LinkNext';
import LinkPrevious from 'grommet/components/icons/base/LinkPrevious';

class ServiceTile extends Component {
    _map() {
        return (
            <Button label='WFS'
                icon={ <LinkNext size="small" /> }
                onClick={ () => {
                          } }
                accent={ true }
                reverse={ true } />
        )
        /*return (
            <Map vertical={ true } data={ { "categories": [{ "id": "source", "label": "Source", "items": [{ "id": "wfs", "label": "WFS", "node": <Button label='WFS' onClick={ () => { } } primary={ false } secondary={ false } accent={ true } plain={ false } /> }] }, { "id": "target", "label": "Target", "items": [{ "id": "ld+json", "label": "ld+json", "node": <Box colorIndex='grey-5' pad='small'> ld+json </Box> }, { "id": "geojson", "label": "geojson", "node": <Box colorIndex='grey-5' pad='small'> geojson </Box> }, { "id": "html", "label": "html", "node": <Box colorIndex='grey-5' pad='small'> html </Box> }] }], "links": [{ "parentId": "wfs", "childId": "ld+json" }, { "parentId": "wfs", "childId": "geojson" }, { "parentId": "wfs", "childId": "html" }] } } />)*/


    }
    render() {
        let item = this.props.item;

        let status = item.status === 'INITIALIZING' ? 'Initializing' : (item.status === 'STARTED' ? 'Online' : 'Offline');
        let icon = item.status === 'INITIALIZING' ? <Spinning size="medium" style={ { verticalAlign: 'middle', marginRight: '6px' } } /> : <StatusIcon value={ item.status === 'STARTED' ? 'ok' : 'critical' } size="medium" />
        return (
            <Tile align="start"
                pad="small"
                direction="column"
                size="large"
                href={ `/services/${item.id}` }
                onClick={ this.props.onClick }
                selected={ this.props.selected }
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
    editable: PropTypes.bool,
    item: PropTypes.object.isRequired,
    onClick: PropTypes.func,
    selected: PropTypes.bool
};

ServiceTile.defaultProps = {
    editable: true
};

// Using export default doesn't seem to pull in the defaultProps correctly
//module.exports = ServiceTile;
export default ServiceTile;