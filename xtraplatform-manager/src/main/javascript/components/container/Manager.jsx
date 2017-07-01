
// TODO: does not work, importing in index.html for now
//import styles
//import 'grommet/scss/vanilla/index';
import '../../scss/default/index';

import React, { Component } from 'react';
import { bindActionCreators } from 'redux'
import { connect } from 'react-redux'
import App from 'grommet/components/App';
import Split from 'grommet/components/Split';
import NavSidebar from '../presentational/NavSidebar'
import { actions, getTitle, getRoutes, getNavActive } from '../../reducers/app'


const mapStateToProps = (state /*, props*/ ) => {
    return {
        title: getTitle(state),
        routes: getRoutes(state),
        navActive: getNavActive(state)
    }
}

const mapDispatchToProps = (dispatch) => ({
    ...bindActionCreators(actions, dispatch)
});

class Manager extends Component {

    render() {
        const {navToggle, navActive} = this.props;

        let nav;
        if (navActive) {
            nav = <NavSidebar title={ this.props.title } routes={ this.props.routes } onClose={ navToggle.bind(null, false) } />;
        }

        return (
            <App centered={ false }>
                <Split flex="right">
                    { nav }
                    { this.props.children }
                </Split>
            </App>
        );
    }
}
;

const ConnectedManager = connect(mapStateToProps, mapDispatchToProps)(Manager)

export default ConnectedManager;
