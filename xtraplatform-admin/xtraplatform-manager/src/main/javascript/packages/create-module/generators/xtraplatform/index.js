const path = require("path");
const fs = require("fs");
const replace = require("replace-in-file");
const merge = require("deepmerge");
const Generator = require("yeoman-generator");
const Neutrino = require("@neutrinojs/create-project/commands/init");
const neutrinoPath = require.resolve(
  "@neutrinojs/create-project/commands/init"
);
const constants = require("@neutrinojs/create-project/commands/init/constants");

const meta = (name) => ({
  name: `@xtraplatform/${name}`,
  version: "1.0.0",
  author: "interactive instruments GmbH",
  license: "MPL-2.0",
  module: "src/index.jsx",
  main: "build/index.js",
  sideEffects: false,
  scripts: {},
  peerDependencies: {},
  devDependencies: {},
});
const scripts = {
  storybook: "start-storybook -p 6006 --docs --ci",
  "build-storybook": "build-storybook --docs",
};
const devDependencies = {
  "@mdx-js/react": "^1.6.16",
  "@storybook/addon-docs": "^6.0.10",
  "@storybook/addon-essentials": "^6.0.10",
  "@storybook/addon-links": "^6.0.10",
  "@storybook/react": "^6.0.10",
  "@xtraplatform/core": "^2.0.0-beta.3",
  "@xtraplatform/neutrino": "^2.0.0-beta.4",
  "feature-u": "^3",
  grommet: "^2",
  "grommet-icons": "^4",
  "react-is": "^16",
  "react-router-dom": "^5",
  "styled-components": "^5",
  "pnp-webpack-plugin": "^1",
  //TODO: needed because eslint shared configs do not work with yarn pnp
  "eslint-plugin-babel": "*",
  "eslint-plugin-import": "*",
  "eslint-plugin-jsx-a11y": "*",
  "eslint-plugin-react": "*",
  "eslint-plugin-react-hooks": "*",
  "eslint-import-resolver-node": "*",
  //TODO: needed for storybook
  "@babel/core": "*",
  "babel-loader": "*",
  "core-js": "*",
  // needed for vscode integration
  eslint: "*",
  prettier: "^2",
};
const peerDependencies = {
  "@xtraplatform/core": "^2",
  "feature-u": "^3",
  grommet: "^2",
  "grommet-icons": "^4",
  "react-is": "^16",
  "react-router-dom": "^5",
  "styled-components": "^5",
};

// use fixed presets
Neutrino.prototype.prompt = function (questions) {
  return {
    projectType: constants.COMPONENTS,
    project: constants.N["REACT_COMPONENTS"],
    testRunner: constants.N["MOCHA"],
    linter: constants.N["AIRBNB"],
  };
};

class XtraPlatform extends Generator {
  constructor(args, opts) {
    super(args, opts);
  }

  writing() {
    const { directory, name } = this.options;

    //patch .neutrinorc.js
    const neutrinorc = this.destinationPath(directory, ".neutrinorc.js");

    this.log(
      "Patching .neutrinorc.js for @xtraplatform",
      this.existsDestination(neutrinorc)
    );

    this.fs.write(
      neutrinorc,
      this.fs
        .read(neutrinorc)
        .replace(
          "const reactComponents = require('@neutrinojs/react-components');",
          "const reactComponents = require('@neutrinojs/react-components');\nconst xtraplatform = require('@xtraplatform/neutrino');"
        )
        .replace(
          "reactComponents(),",
          "reactComponents(),\n    xtraplatform(),"
        )
    );

    //patch package.json
    const packageJson = this.destinationPath(directory, "package.json");

    this.log("Patching package.json for @xtraplatform");

    const patched = merge.all([
      meta(name),
      this.fs.readJSON(packageJson),
      meta(name),
      { scripts },
      { peerDependencies },
      { devDependencies },
    ]);
    //this.log(patched)

    this.fs.writeJSON(packageJson, patched);

    // copy our templates
    this.fs.copyTpl(
      this.templatePath(__dirname, "templates/xtraplatform/**"),
      directory,
      { data: this.options },
      {},
      { globOptions: { dot: true } }
    );

    //yarn.lock
    const yarnLock = this.destinationPath(directory, "yarn.lock");

    this.log("Creating yarn.lock");

    this.writeDestination(yarnLock, "");

    //yarn vscode eslint prettier
    this._spawnSync("yarn dlx @yarnpkg/pnpify --sdk vscode");

    this.log("Activated vscode integrations");
  }
}

XtraPlatform.prototype._spawnSync = Neutrino.prototype._spawnSync;

module.exports = class extends Generator {
  constructor(args, opts) {
    super(args, { ...opts, force: true, debug: true });
    this.argument("directory", { type: String, required: true });
    this.argument("name", { type: String, default: this.options.directory });
  }

  initializing() {
    this.composeWith({ Generator: Neutrino, path: neutrinoPath }, this.options);
    this.composeWith(
      { Generator: XtraPlatform, path: require.resolve("./index.js") },
      this.options
    );
  }
};
