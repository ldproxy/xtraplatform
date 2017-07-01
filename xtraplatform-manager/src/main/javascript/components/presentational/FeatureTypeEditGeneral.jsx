import React, { Component, PropTypes } from 'react';

import Section from 'grommet/components/Section';
import Box from 'grommet/components/Box';
import Heading from 'grommet/components/Heading';
import Form from 'grommet/components/Form';
import FormFields from 'grommet/components/FormFields';
import FormField from 'grommet/components/FormField';
import TextInput from 'grommet/components/TextInput';

class FeatureTypeEditGeneral extends Component {

    constructor(props) {
        super(props);

        this.state = {
            name: props.featureType.name || '',
            displayName: props.featureType.displayName || ''
        }

        this._timers = {}
    }

    _handleInputChange = (event) => {
        const {featureType: {id: id}, onChange} = this.props;

        const target = event.target;
        const value = target.type === 'checkbox' ? target.checked : (event.option ? event.option.value : target.value);
        const field = target.name;

        clearTimeout(this._timers[field]);
        this._timers[field] = setTimeout(() => {
            onChange({
                //id: id,
                [field]: value
            });
        }, 1000);

        this.setState({
            [field]: value
        });

    }

    render() {
        const {name, displayName} = this.state;

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
                                <TextInput name="name" value={ name } disabled={ true } />
                            </FormField>
                            <FormField label="Display name">
                                <TextInput name="displayName" value={ displayName } onDOMChange={ this._handleInputChange } />
                            </FormField>
                        </fieldset>
                    </FormFields>
                </Form>
            </Section>
        );
    }
}

export default FeatureTypeEditGeneral;