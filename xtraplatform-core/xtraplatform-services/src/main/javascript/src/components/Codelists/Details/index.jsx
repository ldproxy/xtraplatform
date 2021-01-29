import React from 'react';
import PropTypes from 'prop-types';
import { useParams, useHistory } from 'react-router-dom';

import { Content, Async } from '@xtraplatform/core';
import Header from './Header';
import Main from './Main';
import { useCodelist } from '../../../hooks';

const CodelistDetails = () => {
    const { id } = useParams();
    const history = useHistory();
    const { loading, error, data } = useCodelist(id);

    const onCancel = () => {
        history.push('/codelists');
    };

    const codelist = data ? data.codelist : {};

    return (
        <Async loading={loading} error={error} noSpinner>
            <Content
                header={<Header {...codelist} onCancel={onCancel} />}
                main={<Main {...codelist} />}
            />
        </Async>
    );
};

CodelistDetails.displayName = 'CodelistDetails';

CodelistDetails.propTypes = {};

export default CodelistDetails;
