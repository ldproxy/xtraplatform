/*
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
define([
    "dojo/_base/declare",
    "xsf/api/Module",
    "xsf/api/Scope",
    "xsf/api/JsonRestSubStoresMixin",
    "xsf/ServicesPageWidget",
    "xsf/SettingsPageWidget",
    "xsf/SettingsModulesWidget",
    "xsf/ServiceEditGeneralWidget",
    "xsf/SettingsGeneralWidget",
    "dojo/i18n"
],
        function(declare, Module, scope, JsonRestSubStoresMixin, ServicesPageWidget, SettingsPageWidget, 
                SettingsModulesWidget, ServiceEditGeneralWidget, 
                SettingsGeneralWidget, i18n) {
            return declare([Module], {
                getMainMenuItems: function() {
                    var userLang = navigator.userLanguage || navigator.language;
                    this.messages = i18n.getLocalization("xsf", "Module", userLang);
                    return [{
                            id: "services",
                            label: this.messages.services,
                            widget: ServicesPageWidget,
                            allwaysRefresh: true,
                            scope: scope.USER,
                            ordinal: 200,
                            forbidden: "_multi_tenancy_root_"
                        },
                        {
                            id: "settings",
                            label: this.messages.settings,
                            widget: SettingsPageWidget, 
                            allwaysRefresh: true,
                            scope: scope.PUBLISHER,
                            ordinal: 300,
                            forbidden: "_multi_tenancy_root_" // currently there are no settings
                        }];
                },
                getTopMenuItems: function() {
                    return [];
                },
                        
                getSettingsItems: function() {
                    var userLang = navigator.userLanguage || navigator.language;
                    this.messages = i18n.getLocalization("xsf", "Module", userLang);
                    return [
                        /*{
                            id: "general",
                            label: this.messages.general,
                            widget: SettingsGeneralWidget,
                            selected: true
                        },
                        {
                            label: 'Modules',
                            widget: SettingsModulesWidget
                        }*/
                    ];
                },
                getServiceSettingsItems: function() {
                    var userLang = navigator.userLanguage || navigator.language;
                    this.messages = i18n.getLocalization("xsf", "Module", userLang);
                    return [
                        {
                            id: "general",
                            label: this.messages.general,
                            selected: true,
                            widget: ServiceEditGeneralWidget
                        }
                    ];
                },
                getServiceViews: function() {
                    return [];
                },
                getSecurityItems: function() {
                    return [];
                },
                getLogItems: function() {
                    return [];
                },
                getServiceActions: function() {
                    return [];
                },
                applyStoreMixins: function(store) {
                    return declare.safeMixin(store, new JsonRestSubStoresMixin());
                }
            });
        });