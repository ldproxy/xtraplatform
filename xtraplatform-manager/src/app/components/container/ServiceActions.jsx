import React, { Component } from 'react';
import { connect } from 'react-redux';

import Header from 'grommet/components/Header';
import Heading from 'grommet/components/Heading';
import Box from 'grommet/components/Box';
import Sidebar from 'grommet/components/Sidebar';
import Menu from 'grommet/components/Menu';
import Button from 'grommet/components/Button';
import SkipLinkAnchor from 'grommet/components/SkipLinkAnchor';
import MapLocation from 'grommet/components/icons/base/MapLocation';
import CloseIcon from 'grommet/components/icons/base/Close';
import CommandLineIcon from 'grommet/components/icons/base/Cli';
import EditIcon from 'grommet/components/icons/base/Edit';
import PowerIcon from 'grommet/components/icons/base/Power';
import TrashIcon from 'grommet/components/icons/base/Trash';
import LayerForm from '../presentational/LayerForm';
import Paragraph from 'grommet/components/Paragraph';


const mapStateToProps = (state /*, props*/ ) => {
    return {

    }
}

class ServiceActions extends Component {

    constructor(props) {
        super(props);

        this.state = {
            layerOpened: false
        }
    }

    _onLayerOpen = () => {
        this.setState({
            layerOpened: true
        });
    }

    _onLayerClose = () => {
        this.setState({
            layerOpened: false
        });
    }

    _onPower = (start) => {
        const {service, updateService} = this.props;

        updateService({
            id: service.id,
            targetStatus: start ? 'STARTED' : 'STOPPED'
        });
    }

    _onRemove = () => {
        const {service, removeService} = this.props;

        removeService({
            id: service.id
        });
    }

    render() {
        const {service, onClose} = this.props;
        const {layerOpened} = this.state;

        let name;
        let closeControl;
        if (onClose) {
            name = <Heading tag="h3" margin='none'>
                       { service.name }
                   </Heading>;
            closeControl = (
                <Button icon={ <CloseIcon /> } onClick={ onClose } a11yTitle={ `Close ${service.name}` } />
            );
        }

        let stateControls;
        if ('STARTED' === service.status) {
            stateControls = [
                /*<Button key="restart"
                    align="start"
                    plain={ true }
                    icon={ <PowerIcon /> }
                    label="Restart"
                    onClick={ this._onLayerOpen.bind(this, 'restart') } />,*/
                <Button key="powerOff"
                    align="start"
                    plain={ true }
                    icon={ <PowerIcon /> }
                    label="Power Off"
                    onClick={ this._onPower.bind(this, false) } />
            ];
        } else {
            stateControls = (
                <Button align="start"
                    plain={ true }
                    icon={ <PowerIcon /> }
                    label="Power On"
                    onClick={ this._onPower.bind(this, true) } />
            );
        }

        let layer;
        if (layerOpened) {
            layer = <LayerForm title="Remove"
                        submitLabel="Yes, remove"
                        compact={ true }
                        onClose={ this._onLayerClose }
                        onSubmit={ this._onRemove }>
                        <fieldset>
                            <Paragraph>
                                Are you sure you want to remove the service with id <strong>{ service.id }</strong>?
                            </Paragraph>
                        </fieldset>
                    </LayerForm>;
        }

        return (
            <Sidebar size="medium" colorIndex="light-2">
                <SkipLinkAnchor label="Right Panel" />
                <Header pad={ { horizontal: 'medium', vertical: 'medium' } } justify="between" size="large">
                    { name }
                    { closeControl }
                </Header>
                <Box pad="medium">
                    <Menu>
                        <Button align="start"
                            plain={ true }
                            icon={ <MapLocation /> }
                            label="View"
                            href={ `/rest/services/${service.id}/` }
                            target="_blank" />
                        { stateControls }
                        { /*<Button align="start"
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            plain={ true }
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            icon={ <EditIcon /> }
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            label="Edit"
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            onClick={ this._onEdit }
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            a11yTitle={ `Edit ${service.name} Virtual Machine` } />*/ }
                        <Button align="start"
                            plain={ true }
                            icon={ <TrashIcon /> }
                            label="Remove"
                            onClick={ this._onLayerOpen }
                            a11yTitle={ `Remove service ${service.name}` } />
                    </Menu>
                </Box>
                { layer }
            </Sidebar>
        );
    }
}


const ConnectedServiceActions = connect(mapStateToProps)(ServiceActions)

export default ConnectedServiceActions