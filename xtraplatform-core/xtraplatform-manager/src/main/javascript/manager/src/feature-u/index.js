export const app = () => `app.cfg`;
export const theme = (name) => `theme.${name || '*'}`;
export const routes = (name) => `routes.${name || '*'}`;
export const i18n = (name) => `i18n.${name || '*'}`;

export const i18nAsResources = (translations) => {
    const resources = {};

    translations.forEach((entry) => {
        Object.keys(entry.translations).forEach((language) => {
            if (!resources[language]) {
                resources[language] = {};
            }

            resources[language][entry.namespace] = entry.translations[language];
        });
    });

    return resources;
};
