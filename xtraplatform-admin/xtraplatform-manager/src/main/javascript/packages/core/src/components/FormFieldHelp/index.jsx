import React from 'react';
import PropTypes from 'prop-types';
import styled from "styled-components";
import { useHover } from '../../hooks'

import { Box, Button, Drop, FormField, Text } from 'grommet';
import { CircleQuestion } from 'grommet-icons';

const HelpIcon = styled(CircleQuestion)`
    pointer-events: none;
`

const FormFieldHelp = ({ label, help, ...rest }) => {
    if (!help || help.length === 0) {
        return (
            <FormField {...rest} label={label} />
        );
    }

    const [hoverRef, isHovered] = useHover();

    const labelWithHelp =
        <Box direction="row" align="center" gap="small">
            {label}
            <Button
                plain
                focusIndicator={false}
                icon={<HelpIcon color="brand" />}
                ref={hoverRef}
            />
            {isHovered && (
                <Drop align={{ left: 'right' }} target={hoverRef.current} plain>
                    <Box
                        margin="xsmall"
                        pad="xsmall"
                        width={{ max: "large" }}
                        background="content"
                        elevation="small"
                        round="small"
                        border={{
                            color: "brand",
                            size: "small",
                        }}
                        animation={["fadeIn"]}
                    >
                        <Text size="small">{help}</Text>
                    </Box>
                </Drop>
            )}
        </Box>

    return (
        <FormField {...rest} label={labelWithHelp} />
    );
};

FormFieldHelp.displayName = 'FormFieldHelp';

FormFieldHelp.propTypes = {
    label: PropTypes.string.isRequired,
    help: PropTypes.string
};

export default FormFieldHelp;
