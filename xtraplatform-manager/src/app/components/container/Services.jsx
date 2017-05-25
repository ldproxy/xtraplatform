import React, { Component } from 'react';
import { bindActionCreators } from 'redux'
import { connect } from 'react-redux'
import ServiceIndex from '../presentational/ServiceIndex'
import { actions as srvcActions, getServices } from '../../reducers/service'
import { actions, getNavActive } from '../../reducers/app'
import { push } from 'react-router-redux'


const mapStateToProps = (state /*, props*/ ) => {
    return {
        services: getServices(state),
        navActive: getNavActive(state),
        messages: state.service.messages
    }
}

const mapDispatchToProps = (dispatch) => ({
    ...bindActionCreators(actions, dispatch),
    ...bindActionCreators(srvcActions, dispatch),
    dispatch
});

class Services extends Component {

    _select = (id) => {
        return () => {
            console.log('selected: ', id);
            // TODO: save in store and push via action, see ferret
            //this.props.dispatch(actions.selectService(id));
            this.props.dispatch(push('/services/' + id));
        };

    }

    render() {
        const {services, navActive, navToggle, messages, clearMessage} = this.props;
        return (
            <ServiceIndex services={ services }
                role="ADMINISTRATOR"
                index={ {} }
                messages={ messages }
                selectService={ this._select }
                navActive={ navActive }
                onNavOpen={ navToggle.bind(null, true) }
                onMessageClose={ clearMessage } />
        );
    }
}
;

const ConnectedServices = connect(mapStateToProps, mapDispatchToProps)(Services)

export default ConnectedServices;
