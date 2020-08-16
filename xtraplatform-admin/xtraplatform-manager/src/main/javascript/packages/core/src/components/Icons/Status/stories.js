import React from 'react';

import StatusIcon from './';

export default {
  title: 'Core/StatusIcon',
  component: StatusIcon,
};

const Template = (args) => <StatusIcon {...args} />;

export const Unknown = Template.bind({});
Unknown.args = {
  value: 'unknown',
};

export const Ok = Template.bind({});
Ok.args = {
  value: 'ok',
};

export const Warning = Template.bind({});
Warning.args = {
  value: 'warning',
};

export const Critical = Template.bind({});
Critical.args = {
  value: 'critical',
};

export const Disabled = Template.bind({});
Disabled.args = {
  value: 'disabled',
};
