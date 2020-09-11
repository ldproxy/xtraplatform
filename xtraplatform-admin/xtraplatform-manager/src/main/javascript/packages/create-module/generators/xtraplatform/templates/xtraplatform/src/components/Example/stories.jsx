import React from 'react';
import { Text } from 'grommet'

import Example from './';

export default {
    title: 'Core/InfoLabel',
    component: Example,
    decorators: [story => <Text>{story()}</Text>]
};

const Template = (args) => <Example {...args} />;


export const Plain = Template.bind({});
