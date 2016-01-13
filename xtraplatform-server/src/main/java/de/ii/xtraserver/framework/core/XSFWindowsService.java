/**
 * Copyright 2016 interactive instruments GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
