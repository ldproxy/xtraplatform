import React from 'react';
import styled from 'styled-components';

import { FileInput } from 'grommet';
import InfoLabel from '../../InfoLabel';
import Field from '../Field';

const StyledFileInput = styled(FileInput)`
    border: 0;
    ${(props) => props.inheritedFrom && `color: ${props.theme.global.colors['dark-6']};`}
`;

const FileField = ({ help, label, inheritedFrom, color, error }) => {
    const File = StyledFileInput;

    // TODO: maybe move to AutoForm

    return (
        <Field label={label} help={help} inheritedFrom={inheritedFrom} color={color} error={error}>
            <File
                name='file'
                accept='.gpkg'
                multiple={false}
                messages={{
                    browse: 'click to browse',
                    dropPrompt: 'Drop file here or ',
                }}
            />
        </Field>
    );
};

FileField.propTypes = {
    ...InfoLabel.propTypes,
};

FileField.defaultProps = {
    ...InfoLabel.defaultProps,
    value: null,
    readOnly: false,
};

FileField.displayName = 'FileField';

export default FileField;
