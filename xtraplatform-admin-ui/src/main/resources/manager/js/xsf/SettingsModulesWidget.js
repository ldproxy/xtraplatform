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
    "dojo/NodeList-dom"
],
function(declare, lang, WidgetBase, dom, domConstruct, domClass, query, array, when, Button, Dialog, Select, DropDownButton, Menu, MenuItem, JsonRest){
    return declare([WidgetBase], {
            
        baseClass: "settingsModulesWidget",
        addServiceDialog: null,
        addServiceButton: null,
        addServiceMenu: null,
        adminStore: null,
        pageWidget: null,
        table: null,
            
        buildRendering: function(){
            this.domNode = domConstruct.create("div", {
                "class": "widget-wrapper",
                style: {
                    textAlign: "center"
                }
            });
            
            this.table = domConstruct.create("table", {"class": "settingsModulesWidget"}, this.domNode);
            var tr =  domConstruct.create("tr", null, this.table);
            domConstruct.create("th", {
                innerHTML: "name"
            }, tr);
            domConstruct.create("th", {
                innerHTML: "version"
            }, tr);
            domConstruct.create("th", {
                innerHTML: "description"
            }, tr);
            domConstruct.create("th", {
                innerHTML: "started"
            }, tr);
            domConstruct.create("th", {
                innerHTML: "enabled"
            }, tr);
            
            this.refreshModules();
        },
            
            refreshModules: function() {
                when(this.adminStore.get("modules"), lang.hitch(this, this._renderModules));
            },
            
            _renderModules: function(modules) {
                array.forEach(modules, function(entry, i){
                    when(this.adminStore.get("modules/" + entry), lang.hitch(this, this._renderModule));
                }, this);
            },
            
            _renderModule: function(module) {
                var tr =  domConstruct.create("tr", null, this.table);
                domConstruct.create("td", {
                    innerHTML: module.name
                }, tr);
                domConstruct.create("td", {
                    innerHTML: module.version
                }, tr);
                domConstruct.create("td", {
                    innerHTML: module.description
                }, tr);
                domConstruct.create("td", {
                    innerHTML: module.started
                }, tr);
                domConstruct.create("td", {
                    innerHTML: module.enabled
                }, tr);
            }
    });
});