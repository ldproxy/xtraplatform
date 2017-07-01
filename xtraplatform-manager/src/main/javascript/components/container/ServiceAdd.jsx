import React, { Component, PropTypes } from 'react';
import { bindActionCreators } from 'redux'
import { connect } from 'react-redux'

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
import TextInput from 'grommet/components/TextInput';
import LinkPreviousIcon from 'grommet/components/icons/base/LinkPrevious';

import { actions } from '../../reducers/service'


const mapStateToProps = (state, props) => ({
});

const mapDispatchToProps = (dispatch) => ({
    ...bindActionCreators(actions, dispatch)
});

class ServiceAdd extends Component {

    constructor(props) {
        super(props);

        this.state = {
            id: '',
            url: ''
        }
    }

    _handleInputChange = (event) => {

        const target = event.target;
        const value = target.type === 'checkbox' ? target.checked : (event.option ? event.option.value : target.value);
        const field = target.name;

        this.setState({
            [field]: value
        });
    }

    _addService = (event) => {
        event.preventDefault();
        const {addService} = this.props;
        const {id, url} = this.state;

        addService({
            id: id,
            wfsUrl: url,
            type: 'ldproxy'
        });
    }

    render() {
        const {id, url} = this.state;

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
                                <TextInput name="id"
                                    autoFocus
                                    value={ id }
                                    onDOMChange={ this._handleInputChange } />
                            </FormField>
                            <FormField label="WFS URL" style={ { width: '100%' } }>
                                <TextInput name="url" value={ url } onDOMChange={ this._handleInputChange } />
                            </FormField>
                        </fieldset>
                    </FormFields>
                    <Footer pad={ { "vertical": "medium" } }>
                        <Button label='Add'
                            type='submit'
                            primary={ true }
                            onClick={ this._addService } />
                    </Footer>
                </Form>
            </div>
        );
    }
}

const ConnectedServiceAdd = connect(mapStateToProps, mapDispatchToProps)(ServiceAdd)

export default ConnectedServiceAdd