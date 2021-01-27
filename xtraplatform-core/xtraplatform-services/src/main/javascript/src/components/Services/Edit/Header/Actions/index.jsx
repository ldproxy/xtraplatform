import React, { useState } from 'react';

import { Box, Anchor, Paragraph, Markdown } from 'grommet';
import { Power as PowerIcon, Trash as TrashIcon } from 'grommet-icons';
import styled from 'styled-components';
import { useTranslation } from 'react-i18next';

import { LayerConfirm } from '@xtraplatform/core';
import { useHistory } from 'react-router-dom';

const Power = styled(Box)`
    & a {
        &:hover {
            & svg {
                stroke: ${(props) => props.theme.global.colors[props.hoverColor]};
            }
        }
    }
`;

const ServiceActions = ({
    id,
    status,
    enabled,
    secured,
    token,
    ViewActions,
    updateService,
    removeService,
}) => {
    const [layerOpened, setLayerOpened] = useState(false);
    const [deletePending, setDeletePending] = useState(false);
    const history = useHistory();
    const { t } = useTranslation();

    const onLayerOpen = () => setLayerOpened(true);

    const onLayerClose = () => setLayerOpened(false);

    const onPower = (start) => {
        updateService({
            enabled: start,
        });
    };

    const onRemove = () => {
        setDeletePending(true);
        removeService(); /*.then(() => {
            //setLayerOpened(false);
            //setDeletePending(false);
            setTimeout(() => history.push('/services'), 2000);
        });*/
    };

    const isOnline = status === 'STARTED';
    const isDisabled = !isOnline && enabled;
    // not needed anymore, handled by cookies
    const parameters = ''; // secured ? `?token=${token}` : ''

    return (
        <Box flex={false}>
            <Box direction='row' justify='end'>
                <Power
                    key='power'
                    hoverColor={
                        isOnline ? 'status-critical' : isDisabled ? 'status-critical' : 'status-ok'
                    }>
                    <Anchor
                        icon={<PowerIcon />}
                        title={`${
                            isOnline
                                ? t('services/ogc_api:services.stop._label')
                                : isDisabled
                                ? 'Defective'
                                : t('services/ogc_api:services.start._label')
                        }`}
                        color={
                            isOnline
                                ? 'status-ok'
                                : isDisabled
                                ? 'status-critical'
                                : 'status-disabled'
                        }
                        onClick={() => onPower(!isOnline)}
                        disabled={isDisabled}
                    />
                </Power>
                {ViewActions.map((ViewAction) => (
                    <ViewAction
                        key={ViewAction.displayName}
                        id={id}
                        isOnline={isOnline}
                        parameters={parameters}
                    />
                ))}
                <Anchor
                    key='remove'
                    icon={<TrashIcon />}
                    title={t('services/ogc_api:services.delete._label')}
                    onClick={onLayerOpen}
                />
            </Box>
            {layerOpened && (
                <LayerConfirm
                    title={t('services/ogc_api:services.delete.confirm._label')}
                    labelConfirm={t('services/ogc_api:services.delete.confirm.proceed')}
                    colorConfirm='status-critical'
                    labelCancel={t('services/ogc_api:services.delete.confirm.cancel')}
                    colorCancel='brand'
                    isPending={deletePending}
                    compact
                    onClose={onLayerClose}
                    onConfirm={onRemove}>
                    <Paragraph>
                        <Markdown>
                            {t('services/ogc_api:services.delete.confirm._description', { id })}
                        </Markdown>
                    </Paragraph>
                </LayerConfirm>
            )}
        </Box>
    );
};

ServiceActions.displayName = 'ServiceActions';

export default ServiceActions;
