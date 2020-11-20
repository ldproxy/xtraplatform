import React, { useState, useContext, useCallback } from 'react';

import { ViewContext } from '../components/Manager/View';

export const useView = () => {
    return useContext(ViewContext);
};

export const useProvideView = (isAdvanced) => {
    const [state, setState] = useState({
        isMenuOpen: false,
        isAdvanced: isAdvanced,
    });

    const toggleMenu = useCallback(() => {
        setState((prevState) => ({ ...prevState, isMenuOpen: !prevState.isMenuOpen }));
    }, [setState]);

    const toggleAdvanced = useCallback(() => {
        setState((prevState) => ({ ...prevState, isAdvanced: !isAdvanced.isAdvanced }));
    }, [setState]);

    return [state, { toggleMenu, toggleAdvanced }];
};
