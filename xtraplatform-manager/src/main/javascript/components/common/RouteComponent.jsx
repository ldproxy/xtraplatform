import React, { Component } from 'react';
import PropTypes from 'prop-types';

export default class RouteComponent extends Component {

    _cleanRoutes = (routes = []) => {
        return routes.map(route => {
            const {component, components, ...rest} = route;
            return rest;
        })
    }

    _getComponentProps = () => {
        const {typedComponents, route, children, ...rest} = this.props;

        return {
            ...rest,
            title: route.title,
            routes: this._cleanRoutes(route.routes)
        }
    }

    render() {
        const {urlQuery: {type}, serviceType, typedComponents, route, children} = this.props;

        let componentProps = {}
        let RouteComp = 'div'

        if (route.component) {
            RouteComp = route.component
            componentProps = this._getComponentProps()
        } else if (route.typedComponent && typedComponents[route.typedComponent]) {
            if (typedComponents[route.typedComponent][serviceType]) {
                RouteComp = typedComponents[route.typedComponent][serviceType]
                componentProps = this._getComponentProps()
            } else if (typedComponents[route.typedComponent][type]) {
                RouteComp = typedComponents[route.typedComponent][type]
                componentProps = this._getComponentProps()
            }
        }

        return (
            <RouteComp {...componentProps}>
                { children }
            </RouteComp>
        )
    }
}
