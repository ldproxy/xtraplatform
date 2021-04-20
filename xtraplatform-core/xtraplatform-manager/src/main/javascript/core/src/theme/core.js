import { css } from 'styled-components';
import { lighten } from 'polished';

export default (base) => ({
    global: {
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
    navigation: {
        background: 'neutral-3',
        dark: true,
        color: {
            dark: 'light-1',
            light: 'dark-1',
        },
        active: {
            color: {
                dark: 'rgba(0,0,0,0.15)',
                light: 'rgba(0,0,0,0.15)',
            },
        },
        overlay: {
            color: {
                dark: 'rgba(0,0,0,0.15)',
                light: 'rgba(0,0,0,0.15)',
            }
        }
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
            size: '12px',
            height: '18px',
        },
    },
    icon: {
        size: {
            list: '16px',
        },
    },
    formField: {
        border: {
            size: 'small',
        },
    },
    list: {
        hover: {
            background: lighten(0.2, base.normalizeColor(base.global.colors.control)),
        },
        selected: {
            background: 'control',
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
