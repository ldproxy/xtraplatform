import { base, grommet } from 'grommet/themes';
import { deepMerge, normalizeColor } from 'grommet/utils';
import core from './core';

export { core };

export const createTheme = (extension = {}) => {
    const intermediateTheme = deepMerge(base, grommet, extension);
    intermediateTheme.normalizeColor = (color) => normalizeColor(color, intermediateTheme);

    const coreTheme = core(intermediateTheme);

    const finalTheme = deepMerge(base, grommet, coreTheme, extension);
    finalTheme.normalizeColor = (color) => normalizeColor(color, finalTheme);

    return finalTheme;
};
