import React, { Component } from 'react';
import { bindActionCreators } from 'redux'
import { connect } from 'react-redux'
import Split from 'grommet/components/Split';
import Article from 'grommet/components/Article';
import Header from 'grommet/components/Header';
import Heading from 'grommet/components/Heading';
import Section from 'grommet/components/Section';
import Box from 'grommet/components/Box';
import Anchor from 'grommet/components/Anchor';
import Button from 'grommet/components/Button';
import Notification from 'grommet/components/Notification';
import List from 'grommet/components/List';
import ListItem from 'grommet/components/ListItem';
import LinkPreviousIcon from 'grommet/components/icons/base/LinkPrevious';
import MoreIcon from 'grommet/components/icons/base/More';
import ListPlaceholder from 'grommet-addons/components/ListPlaceholder';
import ServiceActions from './ServiceActions';
import { push } from 'react-router-redux'
import { actions, getSelectedService, getService, getFeatureTypes } from '../../reducers/service'


const mapStateToProps = (state, props) => {
    return {
        service: getService(state, props.params.id),
        featureTypes: getFeatureTypes(state, props.params.id),
        selectedService: getSelectedService(state)
    }
}

const mapDispatchToProps = (dispatch) => ({
    ...bindActionCreators(actions, dispatch),
    dispatch
});

class ServiceShow extends Component {

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
    componentDidMount() {
        const {selectService, params} = this.props;

        selectService(params.id);
    }

    componentWillReceiveProps(nextProps) {
        const {selectService} = this.props;
        const {params, selectedService} = nextProps;

        if (params && selectedService !== params.id)
            selectService(params.id);
    }

    _onToggleSidebar() {
        this.setState({
            showSidebarWhenSingle: !this.state.showSidebarWhenSingle
        });
    }

    // TODO
    _select = (fid) => {
        return () => {
            var sid = this.props.service.id;
            console.log('selected: ', sid, fid);
            // TODO: save in store and push via action, see ferret
            //this.props.dispatch(actions.selectFeatureType(fid));
            this.props.dispatch(push('/services/' + sid + '/' + fid));
        };

    }

    _renderFeatureTypes() {
        const {featureTypes} = this.props;

        let fts = []

        if (featureTypes)
            fts = featureTypes.map((ft, i) => <ListItem key={ ft.id } separator={ i === 0 ? 'horizontal' : 'bottom' } onClick={ this._select(ft.name) }>
                                                  { ft.displayName }
                                              </ListItem>)
        return (
            <Section pad={ { vertical: 'medium' } } full="horizontal">
                <Box pad={ { horizontal: 'medium' } }>
                    <Heading tag="h2">
                        Feature Types
                    </Heading>
                </Box>
                <List>
                    { fts.length <= 0 ?
                      <ListPlaceholder /> :
                      fts }
                </List>
            </Section>
        );
    }

    render() {
        const {service, role, category, virtualMachine, updateService, removeService} = this.props;
        console.log('loading service ', service ? service.id : 'none');

        let fts;
        fts = this._renderFeatureTypes();

        let sidebar;
        let sidebarControl;
        if ('read only' !== role) {
            let onSidebarClose;
            if ('single' === this.props.responsive) {
                sidebarControl = (
                    <Button icon={ <MoreIcon /> } onClick={ this._onToggleSidebar } />
                );
                onSidebarClose = this._onToggleSidebar;
            }
            sidebar = (
                <ServiceActions category={ category }
                    service={ service }
                    onClose={ onSidebarClose }
                    updateService={ updateService }
                    removeService={ removeService } />
            );
        }

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
                                   { fts }
                               </Article>
                           </div>
                           { sidebar }
                       </Split>
        );
    }
}



const ConnectedServiceShow = connect(mapStateToProps, mapDispatchToProps)(ServiceShow)

export default ConnectedServiceShow