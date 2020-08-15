import React, { useState, useEffect, useRef } from 'react';
import PropTypes from 'prop-types';

import { Box, Form, FormField, TextInput, TextArea } from 'grommet';
import { FormFieldHelp, useDebounceFields } from '@xtraplatform/core'


const ServiceEditGeneral = ({ id, url, label, description, onChange }) => {

  const fields = {
    label: label,
    description: description
  }

  const [state, setState] = useDebounceFields(fields, 2000, onChange);

  return (
    <Box pad={{ horizontal: 'small', vertical: 'medium' }} fill="horizontal">
      <Form>
        <FormFieldHelp label="Id">
          <TextInput name="id" value={id} readOnly={true} />
        </FormFieldHelp>
        <FormFieldHelp label="Url">
          <TextInput name="url" value={url} readOnly={true} />
        </FormFieldHelp>
        <FormFieldHelp label="Label" help="Hilfe Hilfe Hilfe Hilfe Hilfe Hilfe Hilfe Hilfe Hilfe Hilfe Hilfe Hilfe Hilfe Hilfe Hilfe Hilfe Hilfe Hilfe Hilfe Hilfe Hilfe Hilfe Hilfe Hilfe Hilfe Hilfe Hilfe Hilfe Hilfe Hilfe Hilfe Hilfe Hilfe Hilfe Hilfe Hilfe Hilfe Hilfe Hilfe Hilfe Hilfe Hilfe Hilfe ">
          <TextInput name="label" value={state.label} onChange={setState} />
        </FormFieldHelp>
        <FormFieldHelp label="Description" help="HELP">
          <TextArea name="description" value={state.description} onChange={setState} />
        </FormFieldHelp>
      </Form>
    </Box>
  );
};

ServiceEditGeneral.displayName = 'ServiceEditGeneral';

ServiceEditGeneral.propTypes = {
  id: PropTypes.string.isRequired,
  url: PropTypes.string.isRequired,
  label: PropTypes.string,
  description: PropTypes.string,
  onChange: PropTypes.func.isRequired,
};

export default ServiceEditGeneral;
