// (C) Copyright 2014-2016 Hewlett Packard Enterprise Development LP

import React, { Children, Component } from 'react';
import ReactPropTypes from 'prop-types';
import { connect } from 'react-redux'
import classnames from 'classnames';
import { schema, PropTypes } from 'react-desc';
import LinkNextIcon from 'grommet/components/icons/base/LinkNext';
import { Link } from 'redux-little-router';

import CSSClassnames from 'grommet/utils/CSSClassnames';

const CLASS_ROOT = CSSClassnames.ANCHOR;


@connect(
    (state, props) => {
        return {
            isActive: props.path && state.router.pathname && state.router.pathname.indexOf(props.path) === 0
        }
    },
    (dispatch) => {
        return {
        }
    })

export default class Anchor extends Component {

    render() {
        const {a11yTitle, align, animateIcon, children, className, disabled, href, icon, isActive, label, onClick, path, primary, reverse, tag, ...props} = this.props;
        delete props.method;

        let anchorIcon;
        if (icon) {
            anchorIcon = icon;
        } else if (primary) {
            anchorIcon = (
                <LinkNextIcon a11yTitle='link next' />
            );
        }

        if (anchorIcon && !primary && !label) {
            anchorIcon = <span className={ `${CLASS_ROOT}__icon` }>{ anchorIcon }</span>;
        }

        let hasIcon = anchorIcon !== undefined;
        let anchorChildren = Children.map(children, child => {
            if (child && child.type && child.type.icon) {
                hasIcon = true;
                child = <span className={ `${CLASS_ROOT}__icon` }>{ child }</span>;
            }
            return child;
        });

        const target = path ? path.path || path : undefined;
        let adjustedHref = target || href;

        let classes = classnames(
            CLASS_ROOT,
            {
                [`${CLASS_ROOT}--animate-icon`]: hasIcon && animateIcon !== false,
                [`${CLASS_ROOT}--disabled`]: disabled,
                [`${CLASS_ROOT}--icon`]: anchorIcon || hasIcon,
                [`${CLASS_ROOT}--icon-label`]: hasIcon && label,
                [`${CLASS_ROOT}--align-${align}`]: align,
                [`${CLASS_ROOT}--primary`]: primary,
                [`${CLASS_ROOT}--reverse`]: reverse,
                [`${CLASS_ROOT}--active`]: isActive
            },
            className
        );

        let adjustedOnClick = onClick;

        if (!anchorChildren) {
            anchorChildren = label;
        }

        const first = reverse ? anchorChildren : anchorIcon;
        const second = reverse ? anchorIcon : anchorChildren;

        const Component = tag;

        return (
            <Component {...props}
                href={ adjustedHref }
                className={ classes }
                aria-label={ a11yTitle }
                onClick={ adjustedOnClick }>
                { first }
                { second }
            </Component>
        );
    }
}
;

schema(Anchor, {
    description: `A text link. We have a separate component from the browser
  base so we can style it. You can either set the icon and/or label properties
  or just use children.`,
    usage: `import Anchor from 'grommet/components/Anchor';
  <Anchor href={location} label="Label" />`,
    props: {
        a11yTitle: [PropTypes.string, 'Accessibility title.'],
        align: [PropTypes.oneOf(['start', 'center', 'end']), 'Text alignment.'],
        animateIcon: [PropTypes.bool, 'Whether to animate the icon on hover.', {
            defaultProp: true
        }],
        disabled: [PropTypes.bool, 'Whether to disable the anchor.'],
        href: [PropTypes.string, 'Hyperlink reference to place in the anchor. If'
        + ' `path` prop is provided, `href` prop will be ignored.'],
        icon: [PropTypes.element, 'Icon element to place in the anchor.'],
        id: [PropTypes.string, 'Anchor identifier.'],
        label: [PropTypes.node, 'Label text to place in the anchor.'],
        method: [PropTypes.oneOf(['push', 'replace']),
            'Valid only when used with path. Indicates whether the browser history' +
            ' should be appended to or replaced.', {
                defaultProp: 'push'
            }
        ],
        onClick: [PropTypes.func, 'Click handler.'],
        path: [
            PropTypes.oneOfType([PropTypes.object, PropTypes.string]),
            'React-router path to navigate to when clicked.' +
            ' Use path={{ path: ' / ', index: true }} if you want the Anchor to be' +
            ' active only when the index route is current.'
        ],
        primary: [PropTypes.bool, 'Whether this is a primary anchor.'],
        reverse: [
            PropTypes.bool,
            'Whether an icon and label should be reversed so that the icon is at ' +
            'the end of the anchor.'
        ],
        tag: [PropTypes.oneOfType([PropTypes.object, PropTypes.func, PropTypes.string]),
            'The DOM tag to use for the element. The default is <a>. This should be' +
            ' used in conjunction with components like Link from React Router. In' +
            ' this case, Link controls the navigation while Anchor controls the' +
            ' styling.', {
                defaultProp: Link
            }
        ],
        target: [PropTypes.string, 'Target of the link.']
    }
});
