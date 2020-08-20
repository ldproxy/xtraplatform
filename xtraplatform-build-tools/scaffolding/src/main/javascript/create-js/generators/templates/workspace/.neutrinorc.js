const airbnb = require('@neutrinojs/airbnb');
const mocha = require('@neutrinojs/mocha');
const xtraplatform = require('@xtraplatform/neutrino');

module.exports = {
    options: {
        root: __dirname,
    },
    use: [airbnb(), mocha(), xtraplatform()],
};
