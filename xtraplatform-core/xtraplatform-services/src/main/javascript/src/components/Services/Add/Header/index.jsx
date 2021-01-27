import React from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';

import { Box, Anchor } from 'grommet';
import { ChapterAdd, Close } from 'grommet-icons';
import { Header, SpinnerIcon } from '@xtraplatform/core';

const ServiceAddHeader = ({ loading, onCancel }) => {
    const { t } = useTranslation();

    return (
        <Header
            icon={<ChapterAdd />}
            label={t('services/ogc_api:services.add._label')}
            actions={
                <Box direction='row'>
                    {loading ? (
                        <Box pad='small'>
                            <SpinnerIcon size='medium' />
                        </Box>
                    ) : (
                        <Anchor
                            icon={<Close />}
                            title={t('services/ogc_api:manager.back._label', {
                                label: t('services/ogc_api:services._label'),
                            })}
                            onClick={onCancel}
                        />
                    )}
                </Box>
            }
        />
    );
};

ServiceAddHeader.displayName = 'ServiceAddHeader';

ServiceAddHeader.propTypes = {
    onCancel: PropTypes.func,
};

export default ServiceAddHeader;
