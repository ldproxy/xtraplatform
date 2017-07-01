import React, { Component, PropTypes } from 'react';
import SVGIcon from 'grommet/components/SVGIcon';

const CLASS_ROOT = 'ferret-logo';

class Logo extends Component {
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
                viewBox="0 0 140 140"
                version='1.1'
                type='logo'
                a11yTitle='Ferret Logo'>
                <g fill='none'>
                    <rect stroke="none"
                        x="0"
                        y="0"
                        width="140"
                        height="140"></rect>
                    <g className="paths" strokeWidth="10">
                        <rect x="5"
                            y="5"
                            width="10"
                            height="130"></rect>
                        <rect x="125"
                            y="5"
                            width="10"
                            height="130"></rect>
                        <rect x="25"
                            y="5"
                            width="90"
                            height="10"></rect>
                        <rect x="25"
                            y="125"
                            width="90"
                            height="10"></rect>
                        <rect x="25"
                            y="5"
                            width="10"
                            height="130"></rect>
                        <rect x="105"
                            y="5"
                            width="10"
                            height="130"></rect>
                        <rect x="65"
                            y="5"
                            width="10"
                            height="130"></rect>
                        <rect x="45"
                            y="45"
                            width="10"
                            height="10"></rect>
                        <rect x="85"
                            y="45"
                            width="10"
                            height="10"></rect>
                    </g>
                </g>
            </SVGIcon>
        );
    }

}

Logo.propTypes = {
    busy: PropTypes.bool,
    colorIndex: PropTypes.string,
    size: PropTypes.oneOf(['medium', 'large'])
};

Logo.defaultProps = {
    colorIndex: 'brand'
};

export default Logo;