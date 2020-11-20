import React, { useState } from 'react';
import PropTypes from 'prop-types';

import { Box, Form, Button } from 'grommet';
import {
    AutoForm,
    TextField,
    ToggleField,
    useQuery,
    validate,
    minLength,
    maxLength,
    allowedChars,
    objectMap,
} from '@xtraplatform/core';

//TODO: from fassets per type
import OgcApi, {
    fieldsInitial as fieldsInitialOgcApi,
    fieldsValidation as fieldsValidationOgcApi,
} from './OgcApi';

const fieldsInitial = {
    ...fieldsInitialOgcApi,
};

const fieldsValidation = {
    id: [minLength(3), maxLength(32), allowedChars('A-Za-z0-9-_')],
    ...fieldsValidationOgcApi,
};

const ServiceAddMain = ({ loading, error, addService }) => {
    const type = 'OGC_API'; //const { type } = useQuery();
    const [values, setValues] = useState(fieldsInitial);

    const { valid, errors } = validate(values, fieldsValidation);

    const onSubmit = () => {
        if (type) {
            if (valid) {
                addService({
                    ...values,
                    serviceType: type.toUpperCase(),
                    providerType: 'FEATURE',
                });
            } else {
                const initMissingValues = objectMap(fieldsValidation, (key) => '');
                setValues({ ...initMissingValues, ...values });
            }
        }
    };

    return (
        <Box
            pad={{ horizontal: 'small', vertical: 'medium' }}
            fill='horizontal'
            overflow={{ vertical: 'auto' }}>
            <Form value={values} onChange={setValues} onSubmit={onSubmit}>
                <TextField
                    name='id'
                    label='Id'
                    help='The unique identifier of the new service'
                    disabled={loading}
                    autofocus
                    required
                    minLength='3'
                    error={errors['id']}
                />
                <OgcApi {...values} loading={loading} errors={errors} />
                <Box pad={{ vertical: 'medium' }}>
                    <Button label='Add' primary={true} onClick={onSubmit} disabled={loading} />
                </Box>
            </Form>
        </Box>
    );
};

ServiceAddMain.displayName = 'ServiceAddMain';

ServiceAddMain.propTypes = {};

export default ServiceAddMain;
