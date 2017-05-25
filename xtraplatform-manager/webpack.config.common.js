const resolve = require('path').resolve;
const webpack = require('webpack');
const HtmlWebpackPlugin = require('html-webpack-plugin');

module.exports = function() {
return {
    context: resolve(__dirname, 'src/app'),

    entry: [
        './index.jsx'
    ],
    output: {
        filename: '[name].js',
        path: resolve(__dirname, 'dist')
    },

    devtool: 'eval',

    watchOptions: {
        ignored: /node_modules/,
    },

    resolve: {
        extensions: [".js", ".jsx", ".json", ".css", ".scss"]
    },

    module: {
        rules: [
            {
                test: /\.jsx?$/,
                use: [{
                    loader: 'babel-loader',
                }],
                exclude: /(node_modules)/
            },
            {
                test: /\.scss$/,
                use: [
                    {
                        loader: 'style-loader'
                    }, {
                        loader: 'css-loader'/*,
                        options: {
                            modules: true
                        }*/
                    }, /*{
              loader: 'postcss-loader'
            },*/ {
                        loader: 'sass-loader',
                        options: {
                            includePaths: [resolve(__dirname, 'node_modules')]
                        }
                    }
                ],
            },
        ],
    },

    plugins: [

        new webpack.optimize.CommonsChunkPlugin({
            name: 'vendor',
            minChunks: function(module) {
                // this assumes your vendor imports exist in the node_modules directory
                return module.context && module.context.indexOf('node_modules') !== -1;
            }
        }),
        new webpack.optimize.CommonsChunkPlugin({
            name: 'manifest',
        }),

        new HtmlWebpackPlugin({
            title: 'XtraPlatform Manager',
            //favicon: 'assets/img/favicon.png',
            template: 'index.html'
        }),

    ],
}
};
