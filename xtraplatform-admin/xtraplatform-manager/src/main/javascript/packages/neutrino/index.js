const PnpWebpackPlugin = require(`pnp-webpack-plugin`);
const merge = require('deepmerge');

const knownModulePrefixes = ['xtraplatform', '@xtraplatform']
const defaultOptions = {
  modulePrefixes: [],
  lib: true
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

  neutrino.config
    .module
    .rule('lint')
    .use('eslint')
    .tap(options => merge(options, {
      rules: {
        'react/jsx-props-no-spreading': 'off'
      }
    }));

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

  neutrino.config
    .devServer
    .proxy({
      "/rest": {
        target: "http://localhost:7080",
        changeOrigin: true,
        logLevel: 'debug',
        /*onProxyReq(proxyReq, req, res) {
          if (req.method == "POST") {
            console.log('BODY', req.body)
            proxyReq.write(JSON.stringify(req.body));
            proxyReq.end();

          }
        }*/
      },
      "/system": {
        target: "http://localhost:7080",
        changeOrigin: true
      }
    })

};
