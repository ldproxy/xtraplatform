import React, { Component, PropTypes } from 'react';

import Section from 'grommet/components/Section';
import Box from 'grommet/components/Box';
import Heading from 'grommet/components/Heading';

import GrommetTreeList from './GrommetTreeList';

class FeatureTypeEditProperties extends Component {

    render() {
        const {tree, selected, expanded, onExpand, onSelect} = this.props;

        return (
            <Section pad={ { vertical: 'none' } } full="horizontal">
                <Box pad={ { horizontal: 'medium' } }>
                    <Heading tag="h2">
                        Mapping
                    </Heading>
                </Box>
                <GrommetTreeList tree={ tree }
                    expanded={ expanded }
                    selected={ selected }
                    onExpand={ onExpand }
                    onSelect={ onSelect } />
            </Section>
        );
    }
}

export default FeatureTypeEditProperties;
