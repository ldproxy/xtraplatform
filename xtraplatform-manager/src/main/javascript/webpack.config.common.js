const resolve = require('path').resolve;
const webpack = require('webpack');
const HtmlWebpackPlugin = require('html-webpack-plugin');
const ExtractTextPlugin = require("extract-text-webpack-plugin");

module.exports = function(env) {

const extractSass = new ExtractTextPlugin({
    filename: "[name].[contenthash].css",
    disable: env !== "production"
});

return {
    context: resolve(__dirname),

    entry: [
        './index.jsx'
    ],
    output: {
        filename: '[name].js',
        path: resolve('../resources/manager')
    },

    devtool: 'eval',

    watchOptions: {
        ignored: /node_modules/,
    },

    resolve: {
        extensions: [".js", ".jsx", ".json", ".scss"]
    },

    module: {
        rules: [
            {
                test: /\.jsx?$/,
                use: [{
                    loader: 'babel-loader',
                }],
                exclude: /node_modules(?!\/xtraplatform)/
            },
            {
                test: /\.scss$/,
                use: extractSass.extract({
                    use: [{
                        loader: "css-loader"
                    }, {
                        loader: "sass-loader",
                        options: {
                            includePaths: ["node_modules"]
                        }
                    }],
                    fallback: "style-loader"
                })
            }
        ],
    },

    plugins: [

        new webpack.optimize.CommonsChunkPlugin({
            name: 'vendor',
            minChunks: function(module) {
                return module.context && (module.context.indexOf('node_modules') !== -1 || module.context.indexOf('vendor') !== -1);
            }
        }),
        new webpack.optimize.CommonsChunkPlugin({
            name: 'manifest'
        }),

        new HtmlWebpackPlugin({
            title: 'XtraPlatform Manager',
            //favicon: 'assets/img/favicon.png',
            template: 'index.html'
        }),

        extractSass

    ],
}
};
