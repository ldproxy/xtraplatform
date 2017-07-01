import React, { Component } from 'react';
import PropTypes from 'prop-types';

import TextInput from 'grommet/components/TextInput';

import { handleInputChange } from '../../util'


export default class TextInputUi extends Component {

    _handleInputChange = (event) => {
        const {onChange, onDebounce} = this.props;

        handleInputChange(event, onChange, onDebounce);
    }

    render() {
        const {name, value, onChange, onDebounce, ...attributes} = this.props;

        return (
            <TextInput {...attributes}
                name={ name }
                value={ value }
                onDOMChange={ this._handleInputChange } />
        );
    }
}

TextInputUi.propTypes = {
    name: PropTypes.string.isRequired,
    value: PropTypes.string.isRequired,
    onChange: PropTypes.func.isRequired,
    onDebounce: PropTypes.func
};

TextInputUi.defaultProps = {
};