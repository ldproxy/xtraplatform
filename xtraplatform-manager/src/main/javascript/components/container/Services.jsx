import React, { Component } from 'react';
import PropTypes from 'prop-types';
import { connect } from 'react-redux'
import { connectRequest } from 'redux-query';
import ServiceApi from '../../apis/ServiceApi'

@connect(
    (state, props) => {
        return {
            services: state.entities.services, //getServices(state),
            serviceIds: state.entities.serviceIds,
            serviceType: state.router.params && state.router.params.id && state.entities.services && state.entities.services[state.router.params.id] && state.entities.services[state.router.params.id].type // || 'base'
        }
    }
)

@connectRequest(
    (props) => {
        if (!props.serviceIds) {
            return ServiceApi.getServicesQuery()
        }
        return props.serviceIds.map(id => ServiceApi.getServiceQuery(id))
    })

export default class Services extends Component {

    render() {
        const {services, serviceIds, serviceType, children, ...rest} = this.props;

        const componentProps = {
            services,
            serviceIds,
            serviceType
        }

        const childrenWithProps = React.Children.map(children,
            (child) => React.cloneElement(child, {}, React.cloneElement(React.Children.only(child.props.children), componentProps))
        );

        return <div>
                   { childrenWithProps }
               </div>
    }
}

Services.propTypes = {
    //children: PropTypes.element.isRequired
};

Services.defaultProps = {
};