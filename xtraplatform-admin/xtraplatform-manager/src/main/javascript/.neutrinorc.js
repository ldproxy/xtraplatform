const airbnb = require('@neutrinojs/airbnb');
const reactComponents = require('@neutrinojs/react-components');
const mocha = require('@neutrinojs/mocha');
const xtraplatform = require('./xtraplatform.neutrino');

module.exports = {
  options: {
    root: __dirname,
  },
  use: [
    //    airbnb(),
    reactComponents(),
    mocha(),
    xtraplatform({ lib: true }),
  ],
};
