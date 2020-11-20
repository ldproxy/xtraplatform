import React from 'react';
import PropTypes from 'prop-types';

import { Layer, Box, Heading, Button } from 'grommet';
import { Blank } from 'grommet-icons';
import { Content, SpinnerIcon } from '@xtraplatform/core';

const LayerConfirm = ({
    title,
    labelConfirm,
    colorConfirm,
    colorCancel,
    isPending,
    children,
    onConfirm,
    onClose,
}) => {
    const close = () => isPending || onClose();

    return (
        <Layer position='right' full='vertical' onEsc={close} onClickOutside={close}>
            <Content
                header={
                    <Box
                        pad='medium'
                        direction='row'
                        fill='horizontal'
                        align='center'
                        justify='between'>
                        <Heading level='3' margin='none'>
                            {title}
                        </Heading>
                        {isPending && (
                            <Box>
                                <SpinnerIcon size='medium' />
                            </Box>
                        )}
                    </Box>
                }
                main={
                    <Box pad='medium'>
                        <Box>{children}</Box>
                        <Box
                            as='footer'
                            direction='row'
                            pad={{ vertical: 'medium' }}
                            gap='medium'
                            justify='end'>
                            <Button
                                secondary
                                label='Cancel'
                                color={colorCancel}
                                onClick={onClose}
                                disabled={isPending}
                            />
                            <Button
                                primary
                                label={labelConfirm}
                                color={colorConfirm}
                                onClick={onConfirm}
                                disabled={isPending}
                            />
                        </Box>
                    </Box>
                }
            />
        </Layer>
    );
};

LayerConfirm.propTypes = {
    title: PropTypes.string,
    labelConfirm: PropTypes.string,
    colorConfirm: PropTypes.string,
    colorCancel: PropTypes.string,
    isPending: PropTypes.bool,
    children: PropTypes.element,
    onConfirm: PropTypes.func.isRequired,
    onClose: PropTypes.func.isRequired,
};

LayerConfirm.defaultProps = {
    title: 'Confirmation',
    labelConfirm: 'Confirm',
    colorConfirm: null,
    colorCancel: 'status-critical',
    isPending: false,
    children: null,
};

LayerConfirm.displayName = 'LayerConfirm';

export default LayerConfirm;
