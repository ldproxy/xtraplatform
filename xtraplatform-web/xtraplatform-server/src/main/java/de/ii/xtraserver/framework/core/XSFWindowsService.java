/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraserver.framework.core;

/**
 *
 * @author fischer
 */
public class XSFWindowsService {//extends AbstractService implements ServerLifecycleListener {

    /*private static final LocalizedLogger LOGGER = XSFLogger.getLogger(XSFWindowsService.class);
    private Server server;

    @Override
    public int serviceMain(String[] args) throws ServiceException {
        try {

            XtraServerFrameworkService.serviceMain(args, this);

        } catch (Exception ex) {
            return 1;
        }

        return 0;
    }

    @Override
    public int serviceRequest(int control) throws ServiceException {
        switch (control) {
            case SERVICE_CONTROL_STOP:
            case SERVICE_CONTROL_SHUTDOWN:

                try {
                    LOGGER.info(FrameworkMessages.SHUTTING_DOWN_XTRASERVERFRAMEWORK);
                    server.stop();
                    LOGGER.info(FrameworkMessages.XTRASERVERFRAMEWORK_HAS_STOPPED);
                } catch (Exception ex) {
                    LOGGER.error(FrameworkMessages.ERROR_WHEN_STOPPING_XTRASERVERFRAMEWORK, ex.getMessage(), ex);
                }

                break;
            default:
                break;        }
        return 0;
    }

    @Override
    public void serverStarted(Server server) {
        this.server = server;
    }
*/
}
