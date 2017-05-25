import React, { Component, PropTypes } from 'react';
import SVGIcon from 'grommet/components/SVGIcon';

const CLASS_ROOT = 'ferret-logo';

class Logo3 extends Component {
    render() {
        const {busy, className, colorIndex, size, ...props} = this.props;
        let classes = [CLASS_ROOT];
        if (busy) {
            classes.push(`${CLASS_ROOT}--busy`);
        }
        if (className) {
            classes.push(className);
        }
        return (
            <SVGIcon {...props}
                className={ classes.join('') }
                colorIndex={ colorIndex }
                size={ size }
                viewBox="0 0 100 100"
                version='1.1'
                type='logo'
                a11yTitle='Ferret Logo'>
                <g fill='none'>
                    <rect stroke="none"
                        x="0"
                        y="0"
                        width="100"
                        height="100"></rect>
                    <g className="paths" strokeWidth="10">
                        <rect x="85"
                            y="5"
                            width="10"
                            height="90"></rect>
                        <rect x="5"
                            y="85"
                            width="10"
                            height="10"></rect>
                        <rect x="25"
                            y="65"
                            width="10"
                            height="10"></rect>
                        <rect x="45"
                            y="45"
                            width="10"
                            height="10"></rect>
                        <rect x="65"
                            y="25"
                            width="10"
                            height="10"></rect>
                        <rect x="65"
                            y="65"
                            width="10"
                            height="10"></rect>
                        <rect x="45"
                            y="5"
                            width="10"
                            height="10"></rect>
                        <rect x="25"
                            y="25"
                            width="10"
                            height="10"></rect>
                        <rect x="45"
                            y="85"
                            width="10"
                            height="10"></rect>
                        <rect x="5"
                            y="5"
                            width="10"
                            height="90"></rect>
                    </g>
                </g>
            </SVGIcon>
        );
    }

}

Logo3.propTypes = {
    busy: PropTypes.bool,
    colorIndex: PropTypes.string,
    size: PropTypes.oneOf(['medium', 'large'])
};

Logo3.defaultProps = {
    colorIndex: 'brand'
};

export default Logo3;