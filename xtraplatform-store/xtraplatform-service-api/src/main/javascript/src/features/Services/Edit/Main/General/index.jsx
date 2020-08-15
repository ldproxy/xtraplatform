import React, { useState, useEffect, useRef } from 'react';
import PropTypes from 'prop-types';

import { Box, Form, FormField, TextInput, TextArea } from 'grommet';
import { FormFieldHelp, InfoLabel, useDebounceFields } from '@xtraplatform/core'


const ServiceEditGeneral = ({ id, url, label, description, onChange }) => {

  const fields = {
    label: label,
    description: description
  }

  const [state, setState] = useDebounceFields(fields, 2000, onChange);

  return (
    <Box pad={{ horizontal: 'small', vertical: 'medium' }} fill="horizontal">
      <Form>
        <FormField label={<InfoLabel label="Id" />}>
          <TextInput name="id" value={id} readOnly={true} />
        </FormField>
        <FormField label="Url">
          <TextInput name="url" value={url} readOnly={true} />
        </FormField>
        <FormField label={<InfoLabel label="Label" inheritedFrom="service defaults" help="Hilfe Hilfe Hilfe Hilfe Hilfe Hilfe Hilfe Hilfe Hilfe Hilfe Hilfe Hilfe Hilfe Hilfe Hilfe Hilfe Hilfe Hilfe Hilfe Hilfe Hilfe Hilfe Hilfe Hilfe Hilfe Hilfe Hilfe Hilfe Hilfe Hilfe Hilfe Hilfe Hilfe Hilfe Hilfe Hilfe Hilfe Hilfe Hilfe Hilfe Hilfe Hilfe Hilfe " />}>
          <TextInput name="label" value={state.label} onChange={setState} />
        </FormField>
        <FormField label={<InfoLabel label="Description" help="HELP" />}>
          <TextArea name="description" value={state.description} onChange={setState} />
        </FormField>
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
