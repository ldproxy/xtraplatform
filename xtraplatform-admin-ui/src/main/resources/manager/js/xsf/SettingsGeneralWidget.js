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
        function(declare, lang, WidgetBase, dom, domConstruct, domClass, query, array, when, Button, Dialog, Select, DropDownButton, Menu, MenuItem, JsonRest) {
            return declare([WidgetBase], {
                baseClass: "settingsModulesWidget",
                adminStore: null,
                pageWidget: null,
                buildRendering: function() {
                    this.domNode = domConstruct.create("div", {
                        "class": "widget-wrapper",
                        style: {
                            textAlign: "left"
                        }
                    });

                    domConstruct.create("label", {for : "level", innerHTML: "Log Level&nbsp;&nbsp;"}, this.domNode);
                    this.level = new Select({
                        name: "level",
                        options: [
                            {label: "ERROR", value: "ERROR"},
                            {label: "WARN", value: "WARN"},
                            {label: "INFO", value: "INFO", selected: true},
                            {label: "DEBUG", value: "DEBUG"},
                            {label: "TRACE", value: "TRACE"}
                        ],
                        "class": "inverse",
                        style: "margin-right: 10px;",
                        disabled: true
                    }).placeAt(this.domNode);
                }
            });
        });