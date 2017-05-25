import React, { Component } from 'react';
import { connect } from 'react-redux'
import { Router, Route, IndexRedirect } from 'react-router'
import { getTitle, getRoutes } from '../../reducers/app'
import Manager from './Manager'
import Services from './Services'
import ServiceShow from './ServiceShow'
import ServiceAdd from './ServiceAdd'
import ServiceEdit from './ServiceEdit'
import FeatureTypeShow from './FeatureTypeShow'


const mapStateToProps = (state, props) => {
    return {
        routes: getRoutes(state)
    }
}

class App extends Component {

    render() {
        return (
            <Router history={ this.props.history }>
                <Route path="/" component={ Manager }>
                    <IndexRedirect to="/services" />
                    <Route path="services/add" component={ ServiceAdd } />
                    <Route path="services/edit/*" component={ ServiceEdit } />
                    <Route path="services/:id" component={ ServiceShow } />
                    <Route path="services/:id/:ftid" component={ FeatureTypeShow } />
                    <Route path="services" component={ Services } />
                    { /* this.props.routes.map((route, index) => (
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                      <Route key={ index } path={ route.path } component={ import(route.component) } />
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                  )) */ }
                    <Route path="about" component={ About } />
                </Route>
            </Router>
        );
    }
}
;


const About = () => (
    <div>
        <h2>About</h2>
    </div>
)

const ConnectedApp = connect(mapStateToProps)(App)

export default ConnectedApp