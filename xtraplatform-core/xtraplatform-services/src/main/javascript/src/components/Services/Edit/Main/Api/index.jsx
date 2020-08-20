import React, { useState, useEffect, useRef } from 'react';
import PropTypes from 'prop-types';
import merge from 'deepmerge';

import {
  Box, Form, FormField, TextInput, TextArea, Accordion, AccordionPanel, Text,
} from 'grommet';
import { FormFieldHelp, InfoLabel, useDebounceFields } from '@xtraplatform/core';

const ServiceEditApi = ({ api, collections }) => {
  // TODO: get merged values from backend
  const mergedBuildingBlocks = {};

  api.forEach((ext) => {
    const bb = ext.buildingBlock;
    if (mergedBuildingBlocks[bb]) {
      mergedBuildingBlocks[bb] = merge(mergedBuildingBlocks[bb], ext);
    } else {
      mergedBuildingBlocks[bb] = ext;
    }
  });

  // TODO: apply default values

  console.log('API', mergedBuildingBlocks);
  console.log('COLL', collections);

  /* const fields = {
    label: label,
    description: description
  } */

  // const [state, setState] = useDebounceFields(fields, 2000, onChange);

  return (
    <Box pad={{ horizontal: 'small', vertical: 'medium' }} fill="horizontal">
      <Accordion animate>
        {Object.keys(mergedBuildingBlocks).sort().map((bb) => (
          <AccordionPanel key={bb} focusIndicator={false} label={<Text margin={{ vertical: 'xsmall' }}>{bb}</Text>}>
            <Box pad="medium" background="light-2">
              <Text>One</Text>
            </Box>
          </AccordionPanel>
        ))}
      </Accordion>
    </Box>
  );
};

ServiceEditApi.displayName = 'ServiceEditApi';

ServiceEditApi.propTypes = {
  id: PropTypes.string.isRequired,
  url: PropTypes.string.isRequired,
  label: PropTypes.string,
  description: PropTypes.string,
  onChange: PropTypes.func.isRequired,
};

export default ServiceEditApi;
