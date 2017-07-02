import React, { Component } from 'react';
import { bindActionCreators } from 'redux'
import { connect } from 'react-redux'
import { connectRequest } from 'redux-query';
import normalize from '../../apis/ServiceNormalizer'

import Split from 'grommet/components/Split';
import Article from 'grommet/components/Article';
import Header from 'grommet/components/Header';
import Heading from 'grommet/components/Heading';
import Section from 'grommet/components/Section';
import Box from 'grommet/components/Box';
import Button from 'grommet/components/Button';
import Notification from 'grommet/components/Notification';
import List from 'grommet/components/List';
import ListItem from 'grommet/components/ListItem';
import LinkPreviousIcon from 'grommet/components/icons/base/LinkPrevious';
import MoreIcon from 'grommet/components/icons/base/More';
import ListPlaceholder from 'grommet-addons/components/ListPlaceholder';

import ServiceActions from './ServiceActions';
import ServiceEditGeneral from '../presentational/ServiceEditGeneral';
import Anchor from '../common/AnchorLittleRouter';

import { push } from 'react-router-redux'
import { actions, getSelectedService, getService, getFeatureTypes } from '../../reducers/service'



@connectRequest(
    (props) => ({
        url: `/rest/admin/services/${props.urlParams.id}/config/`,
        transform: (serviceConfig) => normalize([serviceConfig]).entities,
        update: {
            services: (prev, next) => next,
            featureTypes: (prev, next) => next,
            mappings: (prev, next) => next
        }
    }))


@connect(
    (state, props) => {
        return {
            service: state.entities.services ? state.entities.services[props.urlParams.id] : null
        }
    },
    (dispatch) => {
        return {
            ...bindActionCreators(actions, dispatch),
            dispatch
        }
    })

export default class ServiceShow extends Component {

    constructor(props) {
        super(props);

        //this._onResponsive = this._onResponsive.bind(this);
        this._onToggleSidebar = this._onToggleSidebar.bind(this);

        this.state = {
            layerName: undefined,
            showSidebarWhenSingle: false
        };
    }

    // TODO: use some kind of declarative wrapper like refetch
    /*componentDidMount() {
        const {selectService, params} = this.props;

        selectService(params.id);
    }

    componentWillReceiveProps(nextProps) {
        const {selectService} = this.props;
        const {params, selectedService} = nextProps;

        if (params && selectedService !== params.id)
            selectService(params.id);
    }*/

    _onToggleSidebar() {
        this.setState({
            showSidebarWhenSingle: !this.state.showSidebarWhenSingle
        });
    }

    _onChange = (change) => {
        const {service, updateService} = this.props;

        updateService({
            ...change,
            id: service.id
        });
    }

    // TODO: use some kind of declarative wrapper like refetch
    render() {
        const {service, children, updateService, removeService} = this.props;
        console.log('loading service ', service ? service.id : 'none');

        let sidebar;
        let sidebarControl;
        let onSidebarClose;
        if ('single' === this.props.responsive) {
            sidebarControl = (
                <Button icon={ <MoreIcon /> } onClick={ this._onToggleSidebar } />
            );
            onSidebarClose = this._onToggleSidebar;
        }
        sidebar = (
            <ServiceActions service={ service }
                onClose={ onSidebarClose }
                updateService={ updateService }
                removeService={ removeService } />
        );

        return (
            service && <Split flex="left"
                           separator={ true }
                           priority={ this.state.showSidebarWhenSingle ? 'right' : 'left' }
                           onResponsive={ this._onResponsive }>
                           <div>
                               <Header pad={ { horizontal: "small", between: 'small', vertical: "medium" } }
                                   justify="start"
                                   size="large"
                                   colorIndex="light-2">
                                   <Anchor icon={ <LinkPreviousIcon /> } path="/services" a11yTitle="Return" />
                                   <Heading tag="h1"
                                       margin="none"
                                       strong={ true }
                                       truncate={ true }>
                                       { service.name }
                                   </Heading>
                                   { sidebarControl }
                               </Header>
                               <Article pad="none" align="start" primary={ true }>
                                   <Section full="horizontal" pad="none">
                                       <Notification pad="medium" status={ service.status === 'STARTED' ? 'ok' : 'critical' } message={ service.status === 'STARTED' ? 'Online' : 'Offline' } />
                                   </Section>
                                   <ServiceEditGeneral service={ service } onChange={ this._onChange } />
                                   { children }
                               </Article>
                           </div>
                           { sidebar }
                       </Split>
        );
    }
}

