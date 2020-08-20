const eslint = require('@neutrinojs/eslint/package.json');
const airbnb = require('@neutrinojs/airbnb/package.json');
const mocha = require('@neutrinojs/mocha/package.json');
const compile = require('@neutrinojs/compile-loader/package.json');
const storybook = require('@storybook/source-loader/package.json');
const xtraplatform = require('@xtraplatform/neutrino/package.json');
const core = require('@xtraplatform/core/package.json');
const { cyan, green, white, yellow } = require('chalk');
const { existsSync } = require('fs');
const { basename, join, relative } = require('path');
const Generator = require('yeoman-generator');

const NEUTRINO_VERSION = '9.3.0';
const STORYBOOK_VERSION = '6.0.14';
const XTRAPLATFORM_CORE_VERSION = '2.0.0-beta.4';
const XTRAPLATFORM_NEUTRINO_VERSION = '2.0.0-beta.4';

//TODO: move into yeoman generator
if (airbnb.version !== NEUTRINO_VERSION) {
    console.warn('version mismatch');
}
if (storybook.version !== STORYBOOK_VERSION) {
    console.warn('version mismatch');
}
if (xtraplatform.version !== XTRAPLATFORM_NEUTRINO_VERSION) {
    console.warn('version mismatch');
}
if (core.version !== XTRAPLATFORM_CORE_VERSION) {
    console.warn('version mismatch');
}

const devDependencies = {
    '@neutrinojs/airbnb': `^${NEUTRINO_VERSION}`,
    '@neutrinojs/mocha': `^${NEUTRINO_VERSION}`,
    '@xtraplatform/neutrino': `^${XTRAPLATFORM_NEUTRINO_VERSION}`,
    neutrino: `^${NEUTRINO_VERSION}`,
};

Object.keys(airbnb.dependencies).forEach((dep) => {
    if (dep.startsWith('eslint-plugin')) {
        devDependencies[dep] = airbnb.dependencies[dep];
    }
});

Object.keys(eslint.dependencies).forEach((dep) => {
    if (dep.startsWith('eslint-plugin')) {
        devDependencies[dep] = airbnb.dependencies[dep];
    }
});

Object.keys(compile.dependencies).forEach((dep) => {
    devDependencies[dep] = compile.dependencies[dep];
});

Object.keys(storybook.dependencies).forEach((dep) => {
    if (dep === 'core-js' || dep === 'regenerator-runtime') {
        devDependencies[dep] = storybook.dependencies[dep];
    }
});

devDependencies['eslint'] = eslint.peerDependencies['eslint'].substr(
    eslint.peerDependencies['eslint'].lastIndexOf('^')
);

devDependencies['mocha'] = mocha.peerDependencies['mocha'].substr(
    mocha.peerDependencies['mocha'].lastIndexOf('^')
);

const peerDependencies = {
    '@xtraplatform/core': `^${XTRAPLATFORM_CORE_VERSION}`,
};

Object.keys(core.peerDependencies).forEach((dep) => {
    peerDependencies[dep] = core.peerDependencies[dep];
});

console.log(devDependencies);

console.log(peerDependencies);

const LOGO = `
      _                   _       _    __                     
     | |                 | |     | |  / _|                    
__  _| |_ _ __ __ _ _ __ | | __ _| |_| |_ ___  _ __ _ __ ___  
\\ \\/ / __| '__/ _\` | '_ \\| |/ _\` | __|  _/ _ \\| '__| '_ \` _ \\ 
 >  <| |_| | | (_| | |_) | | (_| | |_| || (_) | |  | | | | | |
/_/\\_\\\\__|_|  \\__,_| .__/|_|\\__,_|\\__|_| \\___/|_|  |_| |_| |_|
                   | |                                        
                   |_|                                        
`;

module.exports = class extends Generator {
    constructor(args, opts) {
        super(args, { ...opts, force: true, debug: true });
        /* this.argument('directory', { type: String, required: true });
        this.argument('name', {
            type: String,
            default: this.options.directory,
        }); */
    }

    _spawnSync(cmd) {
        const [command, ...args] = cmd.split(' ');
        const { directory, stdio, debug } = this.options;
        const result = this.spawnCommandSync(command, args, {
            cwd: directory,
            stdio,
            env: process.env,
        });

        if (result.error || result.status !== 0) {
            if (result.error) {
                // The child process failed to start entirely, or timed out.
                this.log.error(result.error);
            }

            this.log.error(`The command "${cmd}" exited unsuccessfully.`);

            if (!debug) {
                this.log.error('Cleaning up the incomplete project directory.');
                removeSync(directory);
                this.log.error(
                    'Try again with the --debug flag for more information and to skip cleanup.'
                );
            }

            process.exit(result.status || 1);
        }

        return result;
    }

    //TODO: workspace in current dir or create new?
    async prompting() {
        const done = this.async();

        this.log(cyan.bold(LOGO));
        this.log(white.bold('Welcome to xtraplatform! 👋'));
        this.log(
            cyan(
                'To help you create your new package, I am going to ask you a few questions.\n'
            )
        );

        this.answers = await this.prompt([
            {
                name: 'type',
                type: 'list',
                message:
                    '🤔  First up, what type of package would you like to create?',
                choices: [
                    {
                        name: 'Workspace (normally in the git root directory)',
                        value: 'workspace',
                    },
                    {
                        name: 'Module (normally nested inside a workspace)',
                        value: 'module',
                    },
                ],
            },
            {
                type: 'input',
                name: 'name',
                message: '🤔  Next, how should the package be named?',
            },
            {
                type: 'input',
                name: 'directory',
                message:
                    '🤔  The directory name (if different than package name)',
                default: (answers) => answers.name,
                when: (answers) => answers.type === 'module',
            },
            {
                type: 'confirm',
                name: 'overwrite',
                message: (answers) =>
                    `🤔  The directory ${cyan.bold(
                        answers.directory
                    )} already exists. Are you sure you want to create a ${cyan.bold(
                        answers.type
                    )} in ${cyan.bold(answers.directory)}?`,
                default: (answers) => answers.name,
                when: (answers) =>
                    answers.type === 'module' &&
                    existsSync(join(process.cwd(), answers.directory)),
            },
        ]);

        if (
            this.answers.type === 'module' &&
            existsSync(join(process.cwd(), this.answers.directory)) &&
            !this.answers.overwrite
        ) {
            process.exit(0);
        }

        this.log(
            `\n👌  ${white.bold(
                'Looks like I have all the info I need. Give me a moment while I create your project!'
            )}\n`
        );

        done();
    }

    writing() {
        console.log(this.answers);

        // create dir

        // create package.json

        // ws touch yarn.lock

        // ws yarn set version berry x2

        //ws append .gitignore

        //ws append .yarnrc.yml

        //copy templates
    }

    install() {
        // run yarn
        // install yarn plugins
        // install vscode sdk
    }

    end() {
        const { type, name, directory } = this.answers;
        /*const { projectType, testRunner, linter } = this.data;

        this.log(`\n${green('Hooray, I successfully created your project!')}`);
        this.log(
            `\nI have added a few ${packageManager()} scripts to help you get started:`
        );
        this.log(
            `  • To build your project run:  ${cyan.bold(
                packageManager('run build')
            )}`
        );

        if (projectType !== LIBRARY) {
            this.log(
                `  • To start your project locally run:  ${cyan.bold(
                    packageManager('start')
                )}`
            );
        }

        if (testRunner !== NONE) {
            this.log(
                `  • To execute tests run:  ${cyan.bold(
                    packageManager('test')
                )}`
            );
        }

        if (linter !== NONE) {
            this.log(
                `  • To lint your project manually run:  ${cyan.bold(
                    packageManager('run lint')
                )}`
            );
            this.log(
                `    You can also fix linting problems with:  ${cyan.bold(
                    packageManager('run lint --fix')
                )}`
            );
        }*/

        if (directory) {
            this.log(
                '\nNow change your directory to the following to get started:'
            );
            this.log(
                `  ${cyan('cd')} ${cyan(relative(process.cwd(), directory))}`
            );
            this.log(`\n\n`);
        }
    }
};
