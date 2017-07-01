import React, { Component, PropTypes } from 'react';
import { bindActionCreators } from 'redux'
import { connect } from 'react-redux'
import ui from 'redux-ui';

import Section from 'grommet/components/Section';
import Box from 'grommet/components/Box';
import Anchor from 'grommet/components/Anchor';
import Header from 'grommet/components/Header';
import Heading from 'grommet/components/Heading';
import Footer from 'grommet/components/Footer';
import Button from 'grommet/components/Button';
import Form from 'grommet/components/Form';
import FormFields from 'grommet/components/FormFields';
import FormField from 'grommet/components/FormField';
import LinkPreviousIcon from 'grommet/components/icons/base/LinkPrevious';


import TextInputUi from '../common/TextInputUi';
import { actions } from '../../reducers/service'



@ui({
    state: {
        id: ''
    }
})

@connect(
    (state, props) => {
        return {}
    },
    (dispatch) => {
        return {
            ...bindActionCreators(actions, dispatch)
        }
    })

export default class ServiceAdd extends Component {

    _addService = (event) => {
        event.preventDefault();
        const {ui, addService} = this.props;

        addService(ui);
    }

    render() {
        const {ui, updateUI} = this.props;
        const {children} = this.props;

        return (
            <div>
                <Header pad={ { horizontal: "small", vertical: "medium" } }
                    justify="between"
                    size="large"
                    colorIndex="light-2">
                    <Box direction="row"
                        align="center"
                        pad={ { between: 'small' } }
                        responsive={ false }>
                        <Anchor icon={ <LinkPreviousIcon /> } path={ '/services' } a11yTitle="Return" />
                        <Heading tag="h1" margin="none">
                            <strong>New Service</strong>
                        </Heading>
                    </Box>
                    { /*sidebarControl*/ }
                </Header>
                <Form compact={ false } plain={ true } pad={ { horizontal: 'large', vertical: 'medium' } }>
                    <FormFields>
                        <fieldset>
                            <FormField label="ID" style={ { width: '100%' } }>
                                <TextInputUi name="id"
                                    autoFocus
                                    value={ ui.id }
                                    onChange={ updateUI } />
                            </FormField>
                            { children }
                        </fieldset>
                    </FormFields>
                    <Footer pad={ { "vertical": "medium" } }>
                        <Button label='Add' primary={ true } onClick={ this._addService } />
                    </Footer>
                </Form>
            </div>
        );
    }
}
