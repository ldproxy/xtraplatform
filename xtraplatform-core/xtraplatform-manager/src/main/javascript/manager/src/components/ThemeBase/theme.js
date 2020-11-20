import { grommet, base } from 'grommet/themes';
import { deepMerge } from 'grommet/utils';
import { css } from 'styled-components';
import Color from 'color';

export const customTheme = deepMerge(grommet, {
    global: {
        colors: {
            active: base.global.colors.brand,
            hover: Color(base.global.colors.brand).lighten(0.4).hex(),
            menu: 'neutral-3',
            content: 'white',
            overlay: 'rgba(0,0,0,0.15)',
        },
        edgeSize: {
            xxlarge: '192px',
        },
        breakpoints: {
            medium: {
                value: 1024,
            },
            large: {
                value: 1440,
            },
            xlarge: {},
        },
    },
    menu: {
        background: 'transparent',
        active: {
            color: 'rgba(0,0,0,0.15)',
        },
    },
    anchor: {
        color: {
            dark: 'light-1',
            light: 'dark-1',
        },
        hover: {
            textDecoration: 'none',
            extend: css`
                ${(props) => `color: ${props.theme.global.colors.brand};`}
                & > svg {
                    ${(props) => `stroke: ${props.theme.global.colors.brand};`}
                }
            `,
        },
    },
    text: {
        large: {
            size: '24px',
        },
        list: {
            size: '16px',
        },
    },
    icon: {
        size: {
            list: '16px',
        },
    },
    formField: {
        border: {
            position: 'outer',
            side: 'bottom',
            size: 'small',
            color: 'light-4',
        },
        extend: {
            background: 'inherit',
        },
    },
    tab: {
        color: 'text',
        active: {
            color: 'control',
        },
        hover: {
            color: 'control',
        },
        border: {
            color: 'text',
            active: {
                color: 'control',
            },
            hover: {
                color: 'control',
            },
        },
    },
    tabs: {
        extend: css`
            & > div:nth-child(2) {
                height: 100%;
            }
        `,
    },
    checkBox: {
        toggle: {
            size: '40px',
        },
        size: '20px',
    },
    textArea: {
        extend: {
            minHeight: base.global.size.xsmall,
        },
    },
});
