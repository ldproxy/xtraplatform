const airbnb = require('@neutrinojs/airbnb');
const reactComponents = require('@neutrinojs/react-components');
const mocha = require('@neutrinojs/mocha');
const PnpWebpackPlugin = require(`pnp-webpack-plugin`);

module.exports = {
  options: {
    root: __dirname,
  },
  use: [
    airbnb(),
    reactComponents(),
    mocha(),
    (neutrino) => {
      neutrino.config.resolve.plugin('pnp').use(PnpWebpackPlugin)
      neutrino.config.resolveLoader.plugin('pnp').use(PnpWebpackPlugin.moduleLoader(module))
    },
  ],
};
