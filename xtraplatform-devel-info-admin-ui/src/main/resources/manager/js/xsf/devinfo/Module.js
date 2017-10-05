/*
 * Copyright 2017 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
define([
    "dojo/_base/declare",
    "xsf/api/Module",
    "xsf/devinfo/DevInfoWidget"
],
        function(declare, Module, DevInfoWidget) {
            return declare([Module], {
                getMainMenuItems: function() {
                    return [];
                },
                getTopMenuItems: function() {
                    return [];
                },
                getRightFooterItems: function() {
                    return [
                        {
                            id: "devinfo",
                            widget: DevInfoWidget
                        }
                    ];
                },  
                getSettingsItems: function() {
                    return [];
                },
                getServiceSettingsItems: function() {
                    return [];
                },
                getServiceViews: function() {
                    return [];
                },
                getServiceActions: function() {
                    return [];
                }
            });
        });