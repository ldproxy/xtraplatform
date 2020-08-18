import React from 'react';
import { Text } from 'grommet';

import InfoLabel from '.';

export default {
    title: 'Core/InfoLabel',
    component: InfoLabel,
    decorators: [(story) => <Text>{story()}</Text>],
};

const data = {
    label: 'Label',
    help: 'This is a help text.',
    inheritedFrom: 'somewhere',
    longHelp:
        'This is a help text. This is a help text. This is a help text. This is a help text. This is a help text. This is a help text. This is a help text. This is a help text. This is a help text. This is a help text.',
};

const Template = (args) => <InfoLabel {...args} />;

export const InheritedWithHelp = Template.bind({});
InheritedWithHelp.args = {
    label: data.label,
    inheritedFrom: data.inheritedFrom,
    help: data.help,
};

export const Plain = Template.bind({});
Plain.args = {
    label: data.label,
};

export const Inherited = Template.bind({});
Inherited.args = {
    label: data.label,
    inheritedFrom: data.inheritedFrom,
};

export const WithHelp = Template.bind({});
WithHelp.args = {
    label: data.label,
    help: data.help,
};

export const WithLongHelp = Template.bind({});
WithLongHelp.args = {
    label: data.label,
    help: data.longHelp,
};
