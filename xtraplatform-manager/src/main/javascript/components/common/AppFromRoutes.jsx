import React, { Component } from 'react';
import { Fragment } from 'redux-little-router';


export default class App extends Component {

    _cleanRoutes = (routes = []) => {
        return routes.map(route => {
            const {component, ...rest} = route;
            return rest;
        })
    }
    _renderRoute = (route, prefix = '') => {
        const {urlParams, applicationName} = this.props;
        const componentProps = {
            urlParams,
            applicationName,
            title: route.title,
            routes: this._cleanRoutes(route.routes)
        }

        const path = `${prefix === '/' ? '' : prefix}${route.path}`
        const RouteComponent = route.component

        return <Fragment key={ path } forRoute={ path }>
                   { RouteComponent && <RouteComponent {...componentProps}>
                                           { route.routes && this._renderRoutes(route.routes, path) }
                                       </RouteComponent> }
               </Fragment>
    }

    _renderRoutes = (routes, prefix = '') => {
        return routes.map((route) => this._renderRoute(route, prefix))
    }

    render() {
        /*return (
            <Router history={ this.props.history }>
                <Route path="/" component={ Manager }>
                    <IndexRedirect to="/services" />
                    <Route path="services/add" component={ ServiceAdd } />
                    <Route path="services/edit/*" component={ ServiceEdit } />
                    <Route path="services/:id" component={ ServiceShow } />
                    <Route path="services/:id/:ftid" component={ FeatureTypeShow } />
                    <Route path="services" component={ Services } />
                    { // this.props.routes.map((route, index) => (
                      //<Route key={ index } path={ route.path } component={ import(route.component) } />
                      //))  }
                    <Route path="about" component={ About } />
                </Route>
            </Router>
        );*/
        const {routes} = this.props;

        return this._renderRoute(routes);

    /*return (
        <Fragment forRoute='/'>
            <Manager>
                <Fragment forRoute='/services/add'>
                    <ServiceAdd {...this.props}/>
                </Fragment>
                <Fragment forRoute='/services/:id'>
                    <ServiceShow {...this.props}/>
                </Fragment>
                <Fragment forRoute='/services'>
                    <Services/>
                </Fragment>
            </Manager>
        </Fragment>
    );*/
    }
}
