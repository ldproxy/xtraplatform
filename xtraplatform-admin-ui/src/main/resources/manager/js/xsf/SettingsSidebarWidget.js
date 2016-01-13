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
    "dojo/dom-class",
    "dojo/query",
    "dojo/_base/array",
    "dojo/when",
    "dojo/store/JsonRest",
    "xsf/TabMenuWidget",
    "xsf/SettingsModulesWidget"
],
        function(declare, lang, WidgetBase, dom, domConstruct, domClass, query, array, when, JsonRest, TabMenuWidget, SettingsModulesWidget) {
            return declare([WidgetBase], {
                baseClass: "settingsSidebarWidget",
                adminStore: null,
                pageWidget: null,
                contentContainer: null,
                settingsItems: [],
                mainMenu: null,
                buildRendering: function() {
                    this.domNode = domConstruct.create("div", {
                        "class": "widget-wrapper",
                        style: {
                            textAlign: "center"
                        }
                    });
                    var menuContainer = domConstruct.create("ul", {"class": 'sideBarMenu'}, this.domNode);
                    this.mainMenu = new TabMenuWidget({
                        tabItems: this.settingsItems,
                        widgetParams: {
                            adminStore: this.adminStore,
                            pageWidget: this.pageWidget
                        },
                        domNode: menuContainer,
                        contentContainer: this.contentContainer
                    });

                    /*domConstruct.create("a", {
                     href: "#",
                     innerHTML: "General",
                     class: "selected"
                     }, domConstruct.create("li", null, menu));
                     
                     domConstruct.create("a", {
                     href: "#",
                     innerHTML: "Modules"
                     }, domConstruct.create("li", null, menu));*/
                },
                refresh: function() {
                    if (this.mainMenu && this.mainMenu.refresh) {
                        this.mainMenu.refresh();
                    }
                }
            });
        });