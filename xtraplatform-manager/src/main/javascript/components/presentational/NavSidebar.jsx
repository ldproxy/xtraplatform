import React, { Component, PropTypes } from 'react';
import Box from 'grommet/components/Box';
import Header from 'grommet/components/Header';
import Footer from 'grommet/components/Footer';
import Title from 'grommet/components/Title';
import Sidebar from 'grommet/components/Sidebar';
import Anchor from 'grommet/components/Anchor';
import Menu from 'grommet/components/Menu';
import Button from 'grommet/components/Button';
import User from 'grommet/components/icons/base/User';
import LinkPreviousIcon from 'grommet/components/icons/base/LinkPrevious';
import MenuIcon from 'grommet/components/icons/base/Menu';
import CloseIcon from 'grommet/components/icons/base/Close';
//import Logo from './Logo2'

class NavSidebar extends Component {
    render() {
        return (
            <Sidebar colorIndex='neutral-1' full={ true }>
                <Header pad='medium' justify='between'>
                    <Title onClick={ this.props.onClose } a11yTitle="Close Menu">
                        <MenuIcon />
                        { /*<Logo colorIndex='light-1' size="small" />*/ }
                        <span>{ this.props.title }</span>
                    </Title>
                    <Button icon={ <CloseIcon /> }
                        onClick={ this.props.onClose }
                        plain={ true }
                        a11yTitle="Close Menu" />
                </Header>
                <Box flex='grow' justify='start'>
                    <Menu fill={ true } primary={ true }>
                        { this.props.routes.map((route, index) => (
                              <Anchor key={ index } path={ route.path } label={ route.label } />
                          )) }
                    </Menu>
                </Box>
                { /*<Footer pad='medium'>
                                                                                                                                                                                                                                                                                    <Button icon={ <User /> } />
                                                                                                                                                                                                                                                                                </Footer>*/ }
            </Sidebar>
        );
    }
}
;

NavSidebar.propTypes = {
    routes: PropTypes.arrayOf(PropTypes.shape({
        path: PropTypes.string,
        label: PropTypes.string
    })),
    onClose: PropTypes.func,
    title: PropTypes.string
};

export default NavSidebar;
