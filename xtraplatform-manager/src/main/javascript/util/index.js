
let _timers = {}

export const handleInputChange = (event, onChange, onDebounce, timeout = 1000) => {
    if (event) {
        const target = event.target;
        const value = target.type === 'checkbox' ? target.checked : (event.option ? event.option.value : target.value);
        const field = target.name;

        if (onDebounce) {
            clearTimeout(_timers[field]);
        }

        if (onChange) {
            onChange(field, value);
        }

        if (onDebounce) {
            _timers[field] = setTimeout(() => {
                onDebounce(field, value)
            }, timeout);
        }
    }
}

export const routesToLittleRouter = (route, routes = {}) => {
    const path = route.parent ? route.path.substr(route.parent.length) : route.path
    return {
        [path]: {
            ...routes[path],
            ..._renderRoutes(route.routes)
        }
    }
}

const _renderRoutes = (routes) => {
    const r = {}

    if (routes) {
        return routes.reduce((r1, r2) => {
            return r2.parent
                ? {
                    ...r1,
                    [r2.parent]: {
                        ...r1[r2.parent],
                        ...routesToLittleRouter(r2, r1)
                    }
                }
                : {
                    ...r1,
                    ...routesToLittleRouter(r2, r1)
                }
        }, r)
    }

    return r
}