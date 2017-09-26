import React, { Component } from 'react';
import { Fragment } from 'redux-little-router';

import RouteComponent from './RouteComponent'

export default class AppFromRoutes extends Component {

    _renderRoute = (route, prefix = '') => {
        const {urlParams, urlQuery, applicationName, serviceTypes, typeLabels, serviceMenu, typedComponents} = this.props;

        const componentProps = {
            urlParams,
            urlQuery,
            applicationName,
            serviceTypes,
            typedComponents,
            typeLabels,
            serviceMenu,
            route
        }

        const path = `${prefix === '/' ? '' : prefix}${route.path}`
        console.log(path)
        return <Fragment key={ path } forRoute={ route.path }>
                   { <RouteComponent {...componentProps}>
                         { route.routes && this._renderRoutes(route.routes, path) }
                     </RouteComponent> }
               </Fragment>
    }

    _renderRoutes = (routes, prefix = '') => {
        return routes.map((route) => this._renderRoute(route, prefix))
    }

    render() {
        const {routes} = this.props;

        return this._renderRoute(routes);
    }
}
