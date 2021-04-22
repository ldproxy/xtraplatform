import React from 'react';
import PropTypes from 'prop-types';
import { useTheme } from 'styled-components';

const Spinner = ({ size, color }) => {
    const theme = useTheme();
    const pixelSize = theme.icon.size[size];
    const fill = theme.normalizeColor(color)

    return (
        <svg version='1.1' viewBox='0 0 32 32' width={pixelSize} height={pixelSize} fill={fill}>
            <path
                opacity='.25'
                d='M16 0 A16 16 0 0 0 16 32 A16 16 0 0 0 16 0 M16 4 A12 12 0 0 1 16 28 A12 12 0 0 1 16 4'
            />
            <path d='M16 0 A16 16 0 0 1 32 16 L28 16 A12 12 0 0 0 16 4z'>
                <animateTransform
                    attributeName='transform'
                    type='rotate'
                    from='0 16 16'
                    to='360 16 16'
                    dur='0.8s'
                    repeatCount='indefinite'
                />
            </path>
        </svg>
    );
};

Spinner.displayName = 'Spinner';

Spinner.propTypes = {
    size: PropTypes.string,
    color: PropTypes.string,
};

Spinner.defaultProps = {
    size: 'medium',
    color: '#333333'
};

export default Spinner;
