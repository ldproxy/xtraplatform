import React, { Component, PropTypes } from 'react';
import { bindActionCreators } from 'redux'
import { connect } from 'react-redux'

import Collapsible from 'grommet/components/Collapsible';
import Notification from 'grommet/components/Notification';
import Paragraph from 'grommet/components/Paragraph';
import Button from 'grommet/components/Button';
import Anchor from 'grommet/components/Anchor';
import CaretNextIcon from 'grommet/components/icons/base/CaretNext';
import CaretDownIcon from 'grommet/components/icons/base/CaretDown';


import './NotificationWithCollapsibleDetails.scss';


class NotificationWithCollapsibleDetails extends Component {

    constructor(props) {
        super(props);

        this.state = {
            detailsOpen: props.open || false,
        }
    }

    _toggle = () => {

        this.setState({
            detailsOpen: !this.state.detailsOpen
        });
    }

    render() {
        const {message, details, ...rest} = this.props;
        const {detailsOpen} = this.state;

        const dtls = details && <Collapsible active={ detailsOpen } margin="none">
                                    { Object.values(details).map((dtl, i) => <Paragraph key={ i } size="medium" margin="none">
                                                                                 { i > 0 && <br/> }
                                                                                 { dtl }
                                                                             </Paragraph>) }
                                </Collapsible>

        const Toggle = detailsOpen ? CaretDownIcon : CaretNextIcon

        const msg = <span style={ { verticalAlign: 'top' } }>{ message }<Anchor icon={ <Toggle size="small" /> }
                                                                                title={ `Show more` }
                                                                                a11yTitle={ `Show more` }
                                                                                onClick={ this._toggle } /></span>

        return (
            <Notification {...rest} message={ msg } className="notification-details">
                { dtls }
            </Notification>
        );
    }
}

export default NotificationWithCollapsibleDetails