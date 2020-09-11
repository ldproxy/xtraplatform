import { useState, useEffect, useRef } from 'react';
import { useLocation } from 'react-router-dom';
import useDeepCompareEffect from 'use-deep-compare-effect';
import qs from 'qs';

const useQuery = () => {
    return qs.parse(useLocation().search, { ignoreQueryPrefix: true });
};

const usePrevious = (value) => {
    // The ref object is a generic container whose current property is mutable ...
    // ... and can hold any value, similar to an instance property on a class
    const ref = useRef();

    // Store current value in ref
    useEffect(() => {
        ref.current = value;
    }, [value]); // Only re-run if value changes

    // Return previous value (happens before update in useEffect above)
    return ref.current;
};

const useDebounceValue = (value, delay, deepCompare) => {
    // State and setters for debounced value
    const [debouncedValue, setDebouncedValue] = useState(value);

    const useCompareEffect = deepCompare ? useDeepCompareEffect : useEffect;

    useCompareEffect(
        () => {
            // Update debounced value after delay
            const handler = setTimeout(() => {
                setDebouncedValue(value);
            }, delay);

            // Cancel the timeout if value changes (also on delay change or unmount)
            // This is how we prevent debounced value from updating if value is changed ...
            // .. within the delay period. Timeout gets cleared and restarted.
            return () => {
                clearTimeout(handler);
            };
        },
        [value, delay] // Only re-call effect if value or delay changes
    );

    return debouncedValue;
};

const useDebounce = (value, delay, onChange, deepCompare) => {
    const change = useDebounceValue(value, delay, deepCompare);

    const isFirstRun = useRef(true);

    useEffect(
        () => {
            if (isFirstRun.current) {
                isFirstRun.current = false;
                return;
            }
            onChange(change);
        },
        [onChange, change] // Only call effect if debounced search term changes
    );
};

const useDebounceFields = (fields, delay, onChange) => {
    const setters = {};
    const state = {};

    // TODO
    for (let i = 0; i < Object.keys(fields).length; i++) {
        const field = Object.keys(fields)[i];
        const [value, setter] = useState(fields[field]);

        setters[field] = setter;
        state[field] = value;
    }

    useDebounce(state, delay, onChange, true);

    //TODO: selects etc.
    const setState = (event) => {
        const field = event.target.name;
        const { value } = event.target;

        setters[field](value);
    };

    return [state, setState];
};

// for icons/svg it only works when setting 'pointer-events: none;' in css
const useHover = () => {
    const [value, setValue] = useState(false);

    const ref = useRef(null);

    const handleMouseOver = () => setValue(true);
    const handleMouseOut = () => setValue(false);

    useEffect(() => {
        const node = ref.current;
        if (node) {
            node.addEventListener('mouseover', handleMouseOver);
            node.addEventListener('mouseout', handleMouseOut);

            return () => {
                node.removeEventListener('mouseover', handleMouseOver);
                node.removeEventListener('mouseout', handleMouseOut);
            };
        }
        return undefined;
    }, []);

    return [ref, value];
};

export {
    useQuery,
    useDeepCompareEffect,
    usePrevious,
    useDebounceValue,
    useDebounce,
    useDebounceFields,
    useHover,
};
