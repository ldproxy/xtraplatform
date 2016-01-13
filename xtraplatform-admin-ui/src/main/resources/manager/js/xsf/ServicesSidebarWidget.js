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
    "dijit/form/Button",
    "dijit/Dialog",
    "dijit/form/Select",
    "dijit/form/DropDownButton",
    "dijit/Menu",
    "dijit/MenuItem",
    "dojo/store/JsonRest",
    "dojo/NodeList-dom",
    "dojo/i18n",
    "dojo/string"
],
        function(declare, lang, WidgetBase, dom, domConstruct, domClass, query,
                array, when, Button, Dialog, Select, DropDownButton,
                Menu, MenuItem, JsonRest, NodeListDom, i18n, string) {
            return declare([WidgetBase], {
                baseClass: "servicesSidebarWidget",
                addServiceDialog: null,
                addServiceButton: null,
                addServiceMenu: null,
                adminStore: null,
                pageWidget: null,
                buildRendering: function() {
                    this.domNode = domConstruct.create("div", {
                        "class": "widget-wrapper",
                        style: {
                            textAlign: "center"
                        }
                    });

                    when(this.adminStore.get("servicetypes"), lang.hitch(this, this._renderServiceMenu));
                },
                _renderServiceMenu: function(servicetypes) {
                    this.messages = i18n.getLocalization("xsf", "Module", navigator.userLanguage || navigator.language);
                    if (servicetypes.length > 0) {
                        if (servicetypes.length === 1) {
                            this.addServiceButton = new Button({
                                iconClass: "icon-plus-sign",
                                label: this.messages.newService,
                                "class": "inverse",
                                onClick: lang.hitch(this, this._loadAddServiceDialog, servicetypes[0].id)
                            }).placeAt(this.domNode);
                        }
                        else {
                            this.addServiceMenu = new Menu({
                                "class": "inverse"
                            });

                            this.addServiceButton = new DropDownButton({
                                iconClass: "icon-plus-sign",
                                label: this.messages.service,
                                "class": "inverse",
                                dropDown: this.addServiceMenu
                            }).placeAt(this.domNode);

                            array.forEach(servicetypes, function(entry, i) {
                                this.addServiceMenu.addChild(new MenuItem({
                                    label: entry.id,
                                    onClick: lang.hitch(this, this._loadAddServiceDialog, entry.id)
                                }));
                            }, this);

                            this.addServiceMenu.startup();
                        }
                        this.addServiceButton.startup();
                    }
                },
                _loadAddServiceDialog: function(type) {
                    require(["xsf/" + type + "/ServiceAddDialogWidget"], lang.hitch(this, this._showAddServiceDialog, type));
                },
                _showAddServiceDialog: function(type, SubServiceAddDialogWidget) {
                    // TODO: what is the bogus idProperty for ???
                    var serviceStore = this.adminStore.getSubStore("services/", {idProperty: "bla"});
                    
                    this.addServiceDialog = new SubServiceAddDialogWidget({
                        adminStore: serviceStore,
                        manualProvider: this.manualProvider,
                        pageWidget: this.pageWidget
                    });
                    this.addServiceDialog.onHide = lang.hitch(this, this._destroyAddServiceDialog);
                    this.addServiceDialog.setServiceType(type);
                    this.addServiceDialog.show();
                },
                _destroyAddServiceDialog: function() {
                    this.addServiceDialog.destroyRecursive();
                }
            });
        });