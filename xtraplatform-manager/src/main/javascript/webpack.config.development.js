const resolve = require('path').resolve;
const webpack = require('webpack');
const webpackMerge = require('webpack-merge');
const commonConfig = require('./webpack.config.common');
const bodyParser = require('body-parser')
const parseBody = bodyParser.json();

module.exports = function(env) {
return webpackMerge.strategy({
    entry: 'prepend'
}
)(commonConfig(env), {
    entry: [
        'react-hot-loader/patch'
    ],
    output: {
        publicPath: '/'
    },

    devtool: 'eval',

    plugins: [
        new webpack.HotModuleReplacementPlugin(),
        new webpack.NamedModulesPlugin()
    ],

    devServer: {
        port: 7090,
        hot: true,
        stats: 'normal',
        contentBase: resolve('../resources/manager'),
        publicPath: '/',
        proxy: {
            "/rest": {
                target: "http://localhost:7080",
                changeOrigin: true,
                logLevel: 'debug',
                onProxyReq(proxyReq, req, res) {
                    if (req.method == "POST") {
                        console.log('BODY', req.body)
                        /*parseBody(req, res, () => {
                            console.log('BODY', req.body)
                            proxyReq.write(req.body);
                            proxyReq.end();
                        })*/
                        proxyReq.write(JSON.stringify(req.body));
                        proxyReq.end();

                    }
                }
            }
        },
        overlay: {
            warnings: true,
            errors: true
        },
        historyApiFallback: true,
        setup: (app) => {
            app.use(parseBody)
        }
    }
})
}
