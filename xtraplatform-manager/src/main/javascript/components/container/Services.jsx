import React, { Component } from 'react';
import { bindActionCreators } from 'redux'
import { connect } from 'react-redux'
import { push } from 'redux-little-router'
import { connectRequest } from 'redux-query';
import normalize from '../../apis/ServiceNormalizer'

import ServiceIndex from '../presentational/ServiceIndex'
import { actions as srvcActions, getServices } from '../../reducers/service'
import { actions, getNavActive } from '../../reducers/app'


@connect(
    (state, props) => {
        return {
            services: state.entities.services, //getServices(state),
            serviceIds: state.entities.serviceIds,
            navActive: getNavActive(state),
            messages: state.service.messages
        }
    },
    {
        ...actions,
        ...srvcActions,
        push
    }
)

@connectRequest(
    (props) => {
        if (!props.serviceIds) {
            return {
                url: `/rest/admin/services/`,
                transform: (serviceIds) => ({
                    serviceIds: serviceIds
                }),
                update: {
                    serviceIds: (prev, next) => next
                }
            }
        }

        return props.serviceIds.map(id => ({
            url: `/rest/admin/services/${id}/`,
            transform: (service) => normalize([service]).entities,
            update: {
                services: (prev, next) => Object.assign({}, prev, next)
            }
        }))
    })

export default class Services extends Component {

    /*_select = (id) => {
        return () => {
            console.log('selected: ', id);
            // TODO: save in store and push via action, see ferret
            //this.props.dispatch(actions.selectService(id));
            this.props.push('/services/' + id);
        };

    }*/

    render() {
        const {services, navActive, navToggle, messages, clearMessage} = this.props;
        return (
            <ServiceIndex services={ services }
                role="ADMINISTRATOR"
                index={ {} }
                messages={ messages }
                changeLocation={ this.props.push }
                navActive={ navActive }
                onNavOpen={ navToggle.bind(null, true) }
                onMessageClose={ clearMessage } />
        );
    }
}
