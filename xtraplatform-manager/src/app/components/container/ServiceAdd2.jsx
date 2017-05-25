import React, { Component } from 'react';
import { connect } from 'react-redux'

import Accordion from 'grommet/components/Accordion';
import AccordionPanel from 'grommet/components/AccordionPanel';
import Paragraph from 'grommet/components/Paragraph';


const mapStateToProps = (state /*, props*/ ) => {
    return {

    }
}

const ServiceAdd = () => (
    <Accordion openMulti={ true }>
        <AccordionPanel heading='First Title'>
            <Accordion openMulti={ true }>
                <AccordionPanel heading='First Title'>
                    <Paragraph>
                        Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud
                        exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat.
                    </Paragraph>
                </AccordionPanel>
                <AccordionPanel heading='Second Title'>
                    <Paragraph>
                        Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud
                        exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat.
                    </Paragraph>
                </AccordionPanel>
                <AccordionPanel heading='Third Title'>
                    <Paragraph>
                        Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud
                        exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat.
                    </Paragraph>
                </AccordionPanel>
            </Accordion>
        </AccordionPanel>
        <AccordionPanel heading='Second Title'>
            <Accordion openMulti={ true }>
                <AccordionPanel heading='First Title'>
                    <Paragraph>
                        Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud
                        exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat.
                    </Paragraph>
                </AccordionPanel>
                <AccordionPanel heading='Second Title'>
                    <Paragraph>
                        Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud
                        exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat.
                    </Paragraph>
                </AccordionPanel>
                <AccordionPanel heading='Third Title'>
                    <Paragraph>
                        Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud
                        exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat.
                    </Paragraph>
                </AccordionPanel>
            </Accordion>
        </AccordionPanel>
        <AccordionPanel heading={ <span>Third Title</span> }>
            <Accordion openMulti={ true }>
                <AccordionPanel heading='First Title' pad="medium" reverse={ true }>
                    <Paragraph>
                        Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud
                        exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat.
                    </Paragraph>
                </AccordionPanel>
                <AccordionPanel heading='Second Title'>
                    <Paragraph>
                        Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud
                        exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat.
                    </Paragraph>
                </AccordionPanel>
                <AccordionPanel heading='Third Title'>
                    <Paragraph>
                        Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud
                        exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat.
                    </Paragraph>
                </AccordionPanel>
            </Accordion>
        </AccordionPanel>
    </Accordion>
)



const ConnectedServiceAdd = connect(mapStateToProps)(ServiceAdd)

export default ConnectedServiceAdd