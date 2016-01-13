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
    "dojo/_base/lang",
    "dijit/_WidgetBase",
    "dojo/dom",
    "dojo/dom-construct",
    "dojo/_base/array",
    "dojo/when",
    "dojo/_base/fx",
    "xsf/FlashMessageMixin",
    "xsf/SettingsSidebarWidget",
    "xsf/SettingsModulesWidget",
    "xsf/api/SecurityScopeDecider"
],
        function(declare, lang, WidgetBase, dom, domConstruct, array, when, fx,
                FlashMessageMixin, SettingsSidebarWidget, SettingsModulesWidget, SSD) {
            return declare([WidgetBase, FlashMessageMixin], {
                baseClass: "servicesSidebarWidget",
                serviceList: null,
                adminStore: null,
                services: [],
                modules: [],
                settingsSidebar: null,
                buildRendering: function() {
                    this.domNode = domConstruct.create("div");
                    var sidebar = domConstruct.create("div", {
                        id: "sidebar",
                        "class": "left"
                    }, this.domNode);
                    var maincontent = domConstruct.create("div", {
                        id: "maincontent"
                    }, this.domNode);

                    this.flashMessageNode = maincontent;


                    var settingsItems = [];

                    array.forEach(this.modules, function(mod, i) {
                        settingsItems = settingsItems.concat(mod.getSettingsItems());
                    }, this);
                    
                    this.settingsSidebar = new SettingsSidebarWidget({
                        adminStore: this.adminStore,
                        contentContainer: maincontent,
                        settingsItems: settingsItems,
                        pageWidget: this
                    }).placeAt(sidebar);
                },
                refresh: function() {
                    if (this.settingsSidebar && this.settingsSidebar.refresh) {
                        this.settingsSidebar.refresh();
                    }
                }

            });
        });

