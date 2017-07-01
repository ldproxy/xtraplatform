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
import Sidebar from 'grommet/components/Sidebar';
import Form from 'grommet/components/Form';
import FormFields from 'grommet/components/FormFields';
import FormField from 'grommet/components/FormField';
import LinkPreviousIcon from 'grommet/components/icons/base/LinkPrevious';
import MoreIcon from 'grommet/components/icons/base/More';
import AddIcon from 'grommet/components/icons/base/Add';
import MinusIcon from 'grommet/components/icons/base/Subtract';
import RadialIcon from 'grommet/components/icons/base/Radial';
import StatusIcon from 'grommet/components/icons/Status';
import Animate from 'grommet/components/Animate';
import Collapsible from 'grommet/components/Collapsible';
import ListPlaceholder from 'grommet-addons/components/ListPlaceholder';

import ServiceActions from './ServiceActions';
import PropertyEdit from '../presentational/PropertyEdit';
import FeatureTypeEditGeneral from '../presentational/FeatureTypeEditGeneral';
import FeatureTypeEditProperties from '../presentational/FeatureTypeEditProperties';
import { actions, getService, getFeatureType, getMappingsForFeatureType, getSelectedService, getSelectedFeatureType, getSelectedProperty } from '../../reducers/service'

import { hashHistory as history } from 'react-router';


const mapStateToProps = (state, props) => {
    console.log('loading feature type ', props.params.id, props.params.ftid);
    return {
        service: getService(state, props.params.id),
        featureType: getFeatureType(state, props.params.id, props.params.ftid),
        mappings: getMappingsForFeatureType(state, props.params.id, props.params.ftid),
        selectedService: getSelectedService(state),
        selectedFeatureType: getSelectedFeatureType(state),
        selectedProperty: getSelectedProperty(state)
    }
}

const mapDispatchToProps = (dispatch) => ({
    ...bindActionCreators(actions, dispatch)
});


class FeatureTypeShow extends Component {

    constructor(props) {
        super(props);
    /*const {featureType, selectedProperty} = props;

    if (selectedProperty) {
        this.state = {
            prop: selectedProperty
        }
    }
    if (featureType) {
        this._initState(featureType);
    } else {
        this.state = {
            prop: null
        }
    }*/
    }

    /*componentWillReceiveProps(nextProps) {
        const {featureTypeOld} = this.props;
        const {featureType} = nextProps;

        if (featureType && (!featureTypeOld || featureType.id !== featureTypeOld.id)) {
            this._initState(featureType, this.state);
        }
    }

    _initState = (featureType, state) => {
        if (featureType) {
            if (state) {
                this.setState({
                    prop: featureType.id
                })
            } else {
                this.state = {
                    prop: featureType.id
                }
            }
        }
    }*/

    // TODO: use some kind of declarative wrapper like refetch
    componentDidMount() {
        const {selectFeatureType, selectService, params, featureType} = this.props;

        selectService(params.id);
        if (featureType)
            selectFeatureType(featureType.id);
    }

    componentWillReceiveProps(nextProps) {
        const {selectFeatureType, selectService} = this.props;
        const {params, selectedService, selectedFeatureType, featureType} = nextProps;

        if (params && selectedService !== params.id)
            selectService(params.id);
        if (featureType && selectedFeatureType !== featureType.id)
            selectFeatureType(featureType.id);
    }

    _onSelect = (selected) => {
        const {selectProperty} = this.props;

        selectProperty(selected);

    /*this.setState({
        prop: selected
    })*/
    }

    _onFeatureTypeChange = (change) => {
        const {service, featureType, updateService} = this.props;

        updateService({
            id: service.id,
            featureTypes: {
                [`${featureType.qn}`]: change
            }
        });
    }

    // TODO: use some kind of declarative wrapper like refetch
    _beautify(path) {
        /// TODO
        //return path.substring(path.lastIndexOf(':') + 1)
        const {service} = this.props;
        let displayKey;
        displayKey = (service.nameSpaces[path.substring(path.lastIndexOf('http:'), path.lastIndexOf(':'))] || 'ns1') + path.substring(path.lastIndexOf(':'))
        if (path.indexOf('@') !== -1) {
            displayKey = '@' + displayKey.replace('@', '');
        }
        return displayKey;
    }

    _renderProperties(featureType, mappings) {
        const properties = Object.keys(mappings).filter((key) => key !== featureType.id);

        /*let tree = properties.map((key) => {
            return {
                _id: key,
                title: this._beautify(mappings[key].qn),
                parent: featureType.id
            }
        })*/
        const expanded = [featureType.id];

        let tree = properties.reduce((leafs, key) => {
            let path = mappings[key].qn
            let parent = featureType.id
            let id = key

            while (path.indexOf('http:') !== path.lastIndexOf('http:')) {
                let i = path.indexOf('http:', 1)
                id = path.substring(0, i - 1)

                if (!leafs.find(leaf => leaf._id === id)) {
                    leafs.push({
                        _id: id,
                        title: this._beautify(id),
                        expandable: true,
                        parent: parent
                    })
                    expanded.push(id)
                }

                parent = id
                path = path.substring(i)
            }

            leafs.push({
                _id: key,
                title: this._beautify(path),
                parent: parent
            })

            return leafs
        }, [])
        // TODO: remove name, type, showIncollection if not used
        tree.unshift({
            _id: featureType.id,
            title: this._beautify(featureType.qn),
            expandable: true,
            parent: null
        })

        return (
            <FeatureTypeEditProperties tree={ tree }
                selected={ featureType.id }
                expanded={ expanded }
                onSelect={ this._onSelect } />
        );
    }

    render() {
        const {featureType, service, mappings, selectedProperty} = this.props;

        //const {prop} = this.state;
        let properties,
            general,
            cleanMapping;
        if (mappings && featureType) {
            properties = this._renderProperties(featureType, mappings);
        }
        if (mappings && selectedProperty && mappings[selectedProperty]) {
            let {id, index, qn, ...rest} = mappings[selectedProperty];
            cleanMapping = rest;
        }
        return (
            (service && featureType) &&
            <Split flex="left"
                separator={ true }
                priority="right"
                onResponsive={ this._onResponsive }>
                <div>
                    <Header pad={ { horizontal: "small", between: 'small', vertical: "medium" } }
                        justify="start"
                        size="large"
                        colorIndex="light-2">
                        <Anchor icon={ <LinkPreviousIcon /> } path={ '/services/' + service.id } a11yTitle="Return" />
                        <Heading tag="h1"
                            margin="none"
                            strong={ true }
                            truncate={ true }>
                            { featureType.displayName }
                        </Heading>
                        { /*sidebarControl*/ }
                    </Header>
                    <Article pad="none" align="start" primary={ true }>
                        <FeatureTypeEditGeneral featureType={ featureType } onChange={ this._onFeatureTypeChange } />
                        { properties }
                        <Box pad={ { vertical: 'medium' } } />
                    </Article>
                </div>
                { (selectedProperty && mappings && mappings[selectedProperty]) ?
                  <PropertyEdit title={ this._beautify(mappings[selectedProperty].qn) }
                      qn={ mappings[selectedProperty].qn }
                      mappings={ cleanMapping }
                      onChange={ this._onFeatureTypeChange }
                      isFeatureType={selectedProperty === featureType.id} />
                  :
                  <Sidebar size="medium" colorIndex="light-2" /> }
            </Split>
        );
    }
}



const ConnectedFeatureTypeShow = connect(mapStateToProps, mapDispatchToProps)(FeatureTypeShow)

export default ConnectedFeatureTypeShow