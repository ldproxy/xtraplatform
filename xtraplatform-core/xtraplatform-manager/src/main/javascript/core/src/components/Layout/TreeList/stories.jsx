import React from 'react';

import TreeList from '.';

export default {
    title: 'core/TreeList',
    component: TreeList,
};

const Template = (args) => <TreeList {...args} />;

export const Plain = Template.bind({});

Plain.args = {
    tree: [
        {
            id: 'root',
            label: 'Root',
            parent: null,
        },
        {
            id: 'level1',
            label: 'Level 1',
            parent: 'root',
        },
    ],
    expanded: ['root'],
};
