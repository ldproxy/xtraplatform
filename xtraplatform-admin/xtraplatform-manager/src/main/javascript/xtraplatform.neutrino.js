const PnpWebpackPlugin = require(`pnp-webpack-plugin`);

const knownModulePrefixes = ['xtraplatform']
const defaultOptions = {
  modulePrefixes: [],
  lib: false
}

module.exports = (opts = defaultOptions) => neutrino => {

  const allowedModulePrefixes = knownModulePrefixes.concat(opts.modulePrefixes);
  const allowedModuleRegex = new RegExp(`^.*?\\/\\.yarn\\/\\$\\$virtual\\/(${allowedModulePrefixes.join('|')}).*?$`);

  neutrino.config
    .resolve
    .plugin('pnp')
    .use(PnpWebpackPlugin);

  neutrino.config
    .resolveLoader
    .plugin('pnp2')
    .use(PnpWebpackPlugin.moduleLoader(module));

  neutrino.config
    .module
    .rule('compile')
    .include
    .add(allowedModuleRegex);

  neutrino.config
    .performance
    .maxEntrypointSize(512000)
    .maxAssetSize(512000);


  if (opts.lib) {
    neutrino.config
      .externals((context, request, callback) => {
        if (request[0] !== '.' && request[0] !== '/') {
          //console.log('IGNORE', request, context); 
          callback(null, 'commonjs ' + request);
          return;
        }
        callback();
      });
  }
};
