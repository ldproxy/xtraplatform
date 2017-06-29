import React, { Component } from 'react';
import Box from 'grommet/components/Box';
import Header from 'grommet/components/Header';
import Title from 'grommet/components/Title';
import Section from 'grommet/components/Section';
import Label from 'grommet/components/Label';
import Search from 'grommet/components/Search';
import Anchor from 'grommet/components/Anchor';
import Button from 'grommet/components/Button';
import Notification from 'grommet/components/Notification';
import AddIcon from 'grommet/components/icons/base/Add';
import MenuIcon from 'grommet/components/icons/base/Menu';
import Tiles from 'grommet/components/Tiles';
import Paragraph from 'grommet/components/Paragraph';
import ServiceTile from './ServiceTile';
import NotificationWithCollapsibleDetails from '../common/NotificationWithCollapsibleDetails';

class ServiceIndex extends Component {

    constructor() {
        super();
        this._onSearch = this._onSearch.bind(this);
        this._onMore = this._onMore.bind(this);
        this._onFilterActivate = this._onFilterActivate.bind(this);
        this._onFilterDeactivate = this._onFilterDeactivate.bind(this);
        this.state = {
            searchText: ''
        };
    }

    componentDidMount() {
        /*this.props.dispatch(loadIndex({
            category: 'virtual-machines',
            sort: 'modified:desc'
        }));*/
        console.log('loading services ...');
    }

    componentWillUnmount() {
        //this.props.dispatch(unloadIndex());
        console.log('unloading services ...');
    }

    _onSearch(event) {
        const {index} = this.props;
        const searchText = event.target.value;
        this.setState({
            searchText
        });
        /*const query = new Query(searchText);
        this.props.dispatch(queryIndex(index, query));*/
        console.log(searchText);
    }

    _onMore() {
        const {index} = this.props;
        //this.props.dispatch(moreIndex(index));
        console.log('getting more services ...');
    }

    _onFilterActivate() {
        this.setState({
            filterActive: true
        });
    }

    _onFilterDeactivate() {
        this.setState({
            filterActive: false
        });
    }

    _renderSection(label, items = [], onMore) {
        const {messages, onMessageClose} = this.props;
        const tiles = Object.keys(items).sort(function(a, b) {
            return items[a].dateCreated < items[b].dateCreated ? 1 : -1
        }).map((key, index) => (
            <ServiceTile key={ key } item={ items[key] } onClick={ this.props.selectService(key) } />
        ));
        let header;
        if (label) {
            header = (
                <Header size='small'
                    justify='start'
                    responsive={ false }
                    separator='top'
                    pad={ { horizontal: 'small' } }>
                    <Label size='small'>
                        { label }
                    </Label>
                </Header>
            );
        }
        return (
            <Section key={ label || 'section' } pad='none' colorIndex="light-1">
                { header }
                { Object.values(messages).map(msg => <NotificationWithCollapsibleDetails key={ msg.id }
                                                         size="medium"
                                                         pad="medium"
                                                         margin={ { bottom: 'small' } }
                                                         status={ msg.status }
                                                         message={ msg.text }
                                                         details={ msg.response.details }
                                                         closer={ true }
                                                         onClose={ () => onMessageClose(msg.id) } />) }
                <Tiles flush={ false }
                    fill={ false }
                    selectable={ false }
                    onMore={ onMore }>
                    { tiles }
                </Tiles>
            </Section>
        );
    }

    /*_renderSections(sortProperty, sortDirection) {
        const {index} = this.props;
        const result = index.result || {
            items: []
        };
        const items = (result.items || []).slice(0);
        let sections = [];

        SECTIONS[sortProperty].forEach((section) => {

            let sectionValue = section.value;
            if (sectionValue instanceof Date) {
                sectionValue = sectionValue.getTime();
            }
            let sectionItems = [];

            while (items.length > 0) {
                const item = items[0];
                let itemValue = (item.hasOwnProperty(sortProperty) ?
                    item[sortProperty] : item.attributes[sortProperty]);
                if (itemValue instanceof Date) {
                    itemValue = itemValue.getTime();
                }

                if (undefined === sectionValue ||
                        ('asc' === sortDirection && itemValue <= sectionValue) ||
                        ('desc' === sortDirection && itemValue >= sectionValue)) {
                    // item is in section
                    sectionItems.push(items.shift());
                } else {
                    // done
                    break;
                }
            }

            if (sectionItems.length > 0) {
                sections.push(this._renderSection(section.label, sectionItems));
            }
        });

        return sections;
    }*/

    render() {
        const {index, role, services, navActive, onNavOpen} = this.props;
        const {filterActive, searchText} = this.state;
        const result = /*index.result ||*/ {
            items: services
        };

        let addControl;
        if ('read only' !== role) {
            addControl = (
                <Anchor icon={ <AddIcon /> } path='/services/add' a11yTitle={ `Add service` } />
            );
        }

        let sections;
        /*let sortProperty,
            sortDirection;
        if (index.sort) {
            [sortProperty, sortDirection] = index.sort.split(':');
        }
        if (sortProperty && SECTIONS[sortProperty]) {
            sections = this._renderSections(sortProperty, sortDirection);
        } else {*/
        let onMore;
        if (result.count > 0 && result.count < result.total) {
            onMore = this._onMore;
        }
        sections = this._renderSection(undefined, result.items, onMore);
        //}

        let navControl;
        if (!navActive) {
            navControl = <Button onClick={ onNavOpen } plain={ true } a11yTitle={ `Open Menu` }>
                             <MenuIcon style={ { verticalAlign: 'middle' } } />
                         </Button>;
        }

        return (
            <Box>
                <Header size='large' pad={ { horizontal: 'medium' } }>
                    <Title responsive={ false }>
                        { navControl }
                        <span>Services</span>
                    </Title>
                    { /*<Search inline={ true }
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        fill={ true }
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        size='medium'
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        placeHolder='Search'
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        value={ searchText }
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        onDOMChange={ this._onSearch } />*/ }
                    { addControl }
                    { /*<FilterControl filteredTotal={ result.total } unfilteredTotal={ result.unfilteredTotal } onClick={ this._onFilterActivate } />*/ }
                </Header>
                { sections }
                { /*<ListPlaceholder filteredTotal={ result.total }
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    unfilteredTotal={ result.unfilteredTotal }
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    emptyMessage='You do not have any virtual machines at the moment.'
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    addControl={ <Button icon={ <AddIcon /> }
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                     label='Add virtual machine'
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                     path='/virtual-machines/add'
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                     primary={ true }
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                     a11yTitle={ `Add virtual machine` } /> } />
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                { filterLayer }*/ }
            </Box>
        );
    }



}
;

export default ServiceIndex;
