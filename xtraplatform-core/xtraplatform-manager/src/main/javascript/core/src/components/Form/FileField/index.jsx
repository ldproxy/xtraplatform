import React from 'react';
import PropTypes from 'prop-types';
import styled from 'styled-components';

import { FileInput } from 'grommet';
import InfoLabel from '../../InfoLabel';
import Field from '../Field';

const StyledFileInput = styled(FileInput)`
    border: 0;
    ${(props) => props.inheritedFrom && `color: ${props.theme.global.colors['dark-6']};`}
`;

const FileField = ({
    help,
    label,
    inheritedFrom,
    color,
    error,
    maxSize,
    name,
    multiple,
    accept,
}) => {
    const File = StyledFileInput;

    // TODO: maybe move to AutoForm

    return (
        <Field label={label} help={help} inheritedFrom={inheritedFrom} color={color} error={error}>
            <File
                name={name}
                accept={accept}
                multiple={multiple}
                messages={{
                    browse: 'click to browse',
                    dropPrompt: 'Drop file here or ',
                }}
                maxSize={maxSize}
            />
        </Field>
    );
};

FileField.propTypes = {
    ...InfoLabel.propTypes,
    maxSize: PropTypes.number,
    accept: PropTypes.string,
    multiple: PropTypes.bool,
    name: PropTypes.string,
};

FileField.defaultProps = {
    ...InfoLabel.defaultProps,
    value: null,
    readOnly: false,
    multiple: false,
    label: 'GeoPackage-Datei',
};

FileField.displayName = 'FileField';

export default FileField;
