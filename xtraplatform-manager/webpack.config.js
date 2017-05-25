const resolve = require('path').resolve;
const webpack = require('webpack');
const webpackMerge = require('webpack-merge');
const commonConfig = require('./webpack.config.common');

module.exports = function(env) {
return webpackMerge.strategy({
    entry: 'prepend'
}
)(commonConfig(), {
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
        contentBase: resolve(__dirname, 'dist'),
        publicPath: '/',
        proxy: {
            "/rest": "http://localhost:7080"
        },
        overlay: {
            warnings: true,
            errors: true
        },
        historyApiFallback: true
    }
})
}
