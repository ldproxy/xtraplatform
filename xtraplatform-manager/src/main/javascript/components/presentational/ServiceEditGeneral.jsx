import React, { Component } from 'react';
import PropTypes from 'prop-types';
import ui from 'redux-ui';

import Section from 'grommet/components/Section';
import Box from 'grommet/components/Box';
import Heading from 'grommet/components/Heading';
import Form from 'grommet/components/Form';
import FormFields from 'grommet/components/FormFields';
import FormField from 'grommet/components/FormField';
import TextInput from 'grommet/components/TextInput';

import TextInputUi from '../common/TextInputUi';

@ui({
    state: {
        name: (props) => props.service.name
    }
})
export default class ServiceEditGeneral extends Component {

    _save = () => {
        const {ui, onChange} = this.props;

        onChange(ui);
    }

    render() {
        const {service, ui, updateUI} = this.props;

        return (
            <Section pad={ { vertical: 'medium' } } full="horizontal">
                <Box pad={ { horizontal: 'medium' } } separator="bottom">
                    <Heading tag="h2">
                        General
                    </Heading>
                </Box>
                <Form compact={ false } pad={ { horizontal: 'medium', vertical: 'small' } }>
                    <FormFields>
                        <fieldset>
                            <FormField label="Id">
                                <TextInput name="id" value={ service.id } disabled={ true } />
                            </FormField>
                            <FormField label="Display name">
                                <TextInputUi name="name"
                                    value={ ui.name }
                                    onChange={ updateUI }
                                    onDebounce={ this._save } />
                            </FormField>
                        </fieldset>
                    </FormFields>
                </Form>
            </Section>
        );
    }
}

ServiceEditGeneral.propTypes = {
    onChange: PropTypes.func.isRequired
};

ServiceEditGeneral.defaultProps = {
};