import React from 'react';
import PropTypes from 'prop-types';
import { useLocation, useHistory } from 'react-router-dom';

import { Content, Async } from '@xtraplatform/core';
import CodelistIndexHeader from './Header';
import CodelistIndexMain from './Main';
import { useCodelists } from '../../../hooks';

const CodelistIndex = ({ isCompact }) => {
    const location = useLocation();
    const history = useHistory();
    const onSelect = (id) => {
        history.push({
            pathname: `${location.pathname}/${id}`,
        });
    };
    const { loading, error, data } = useCodelists();
    const codelists = data ? data.codelists : [];

    return (
        <Content
            header={<CodelistIndexHeader isCompact={isCompact} />}
            main={
                <Async loading={loading} error={error}>
                    <CodelistIndexMain
                        codelists={codelists}
                        isCompact={isCompact}
                        onSelect={onSelect}
                    />
                </Async>
            }
        />
    );
};

CodelistIndex.displayName = 'CodelistIndex';

CodelistIndex.propTypes = {
    isCompact: PropTypes.bool,
};

export default CodelistIndex;
