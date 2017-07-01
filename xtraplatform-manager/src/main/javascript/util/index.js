
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