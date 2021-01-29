import React from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';

import { Box, Anchor } from 'grommet';
import { List, Close } from 'grommet-icons';
import { Header } from '@xtraplatform/core';

const CodelistDetailsHeader = ({ id, label, entries, onCancel }) => {
    const { t } = useTranslation();

    return (
        <Header
            icon={<List />}
            label={label}
            title={`${label} [${id}]`}
            actions={
                <Box direction='row'>
                    <Anchor
                        icon={<Close />}
                        title={t('services/ogc_api:manager.back._label', {
                            label: t('services/ogc_api:codelists._label'),
                        })}
                        onClick={onCancel}
                    />
                </Box>
            }
        />
    );
};

CodelistDetailsHeader.displayName = 'CodelistDetailsHeader';

CodelistDetailsHeader.propTypes = {};

export default CodelistDetailsHeader;
