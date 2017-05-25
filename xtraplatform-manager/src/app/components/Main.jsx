//import styles
import 'grommet/scss/vanilla/index';

import React, { Component } from 'react';
import ReactDOM from 'react-dom';
import App from 'grommet/components/App';
import Box from 'grommet/components/Box';
import Header from 'grommet/components/Header';
import Footer from 'grommet/components/Footer';
import Meter from 'grommet/components/Meter';
import Title from 'grommet/components/Title';
import Value from 'grommet/components/Value';
import Split from 'grommet/components/Split';
import Sidebar from 'grommet/components/Sidebar';
import Anchor from 'grommet/components/Anchor';
import Menu from 'grommet/components/Menu';
import Button from 'grommet/components/Button';
import User from 'grommet/components/icons/base/User';

class Main extends Component {
    render() {
        return (
            <App centered={ false }>
                <Split flex="right">
                    <Sidebar colorIndex='neutral-1' full={ true }>
                        <Header pad='medium' justify='between'>
                            <Title>
                                ldproxy
                            </Title>
                        </Header>
                        <Box flex='grow' justify='start'>
                            <Menu primary={ true }>
                                <Anchor href='#services' className='active'>
                                    Services
                                </Anchor>
                                <Anchor href='#security'>
                                    Security
                                </Anchor>
                                <Anchor href='#logs'>
                                    Logs
                                </Anchor>
                                <Anchor href='#settings'>
                                    Settings
                                </Anchor>
                            </Menu>
                        </Box>
                        <Footer pad='medium'>
                            <Button icon={ <User /> } />
                        </Footer>
                    </Sidebar>
                    <Box colorIndex='neutral-2'
                        justify='center'
                        align='center'
                        pad='medium'>
                        Right Side
                    </Box>
                </Split>
            </App>
        );
    }
}
;
/*
let element = document.getElementById('content');
ReactDOM.render(React.createElement(Main), element);

document.body.classList.remove('loading');
*/
export default Main;
