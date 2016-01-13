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
    "dijit/_WidgetBase",
    "dijit/_TemplatedMixin",
    "dijit/_WidgetsInTemplateMixin",
    "dojo/text!./MappingEditorWidget/template.html",
    "dijit/Dialog",
    "dojo/query",
    "dojo/when",
    "dojo/_base/array",
    "dojo/dom",
    "dojo/dom-style",
    "dojo/dom-construct",
    "dojo/_base/lang",
    "dijit/Tree",
    "dojo/store/Memory",
    "dijit/tree/ObjectStoreModel",
    "dojo/store/JsonRest",
    "dijit/TooltipDialog",
    "dijit/popup",
    "dijit/form/Button",
    "dijit/form/Select",
    "dijit/form/CheckBox",
    "dijit/form/TextBox",
    "dojo/on",
    "xsf/MappingEditorFieldTooltipWidget",
    "dojo/i18n",
    "dojo/string"
],
        function(declare, WidgetBase, TemplatedMixin, WidgetsInTemplateMixin,
                template, Dialog, query, when, array, dom, domStyle,
                domConstruct, lang, Tree, Memory, ObjectStoreModel,
                JsonRest, TooltipDialog, popup, Button, Select,
                CheckBox, TextBox, on, MappingEditorFieldTooltipWidget, i18n, string) {
            return declare([WidgetBase, TemplatedMixin, WidgetsInTemplateMixin], {
                templateString: template,
                dialog: null,
                title: "",
                action: null,
                config: null,
                service: null,
                adminStore: null,
                pageWidget: null,
                tooltip: null,
                useAsId: "",
                mappings: null,
                //style: "width:700px; height: 500px;",
                constructor: function() {
                    this.messages = i18n.getLocalization("xsf", "Module", navigator.userLanguage || navigator.language);
                },
                postMixInProperties: function()
                {
                    this.dialog = new Dialog({
                        title: this.title,
                        style: this.style,
                        parseOnLoad: false,
                        draggable: false
                    });

                    query(".closeText", this.dialog.domNode).addClass("icon-remove-circle").empty();
                    //query(".dijitDialogTitleBar", this.addServiceDialog.domNode).addClass("dijitButtonNode");
                    //domClass.add(this.addServiceDialog.domNode, "inverse");

                    this.inherited(arguments);
                },
                postCreate: function()
                {
                    this.dialog.set("content", this.domNode);

                    if (this.mappings === null) {
                        when(this.adminStore.get(this.service.id + "/" + this.config.id + "/mapping"), lang.hitch(this, this._createTree));
                    } else {
                        this._createTree(this.mappings);
                    }

                    //domConstruct.create("div", {style: "width:100%; margin:0px;", innerHTML: "bla"}, this.contentArea);

                    this.dialog.onCancel = lang.hitch(this, this.hide);
                    this.cancelButton.onClick = lang.hitch(this, this.hide);
                    this.saveButton.onClick = lang.hitch(this, this.save);

                    this.inherited(arguments);
                },
                _replaceAll: function(source, search, replace) {
                    var re = new RegExp(search.replace(/[\-\[\]\/\{\}\(\)\*\+\?\.\\\^\$\|]/g, "\\$&"), "g");
                    return source.replace(re, replace);
                },
                _createTree: function(mappings) {

                    //console.log(mappings);

                    var t = [
                        {
                            id: this.config.name,
                            name: this.config.name
                        }
                    ];
                    for (var path in mappings.mapping) {
                        if (mappings.mapping.hasOwnProperty(path)) {
                            if (mappings.mapping[path][0].fields[0] === -1)
                                continue;
                            var p = path;
                            for (var ns in mappings.namespaces) {
                                if (mappings.namespaces.hasOwnProperty(ns) && mappings.namespaces[ns] !== "") {
                                    p = this._replaceAll(p, mappings.namespaces[ns], ns);
                                }
                            }

                            var fields = [];
                            array.forEach(mappings.mapping[path], function(m, i) {
                                if (!m.pattern && m.fields[0] !== -1) {
                                    array.forEach(m.fields, function(f, j) {
                                        fields.push(this.config.fieldsConfig[f]);
                                        //field = this.config.fieldsConfig[f.fields[0]];
                                    }, this);
                                    return;
                                }
                            }, this);

                            var a = p.split('/');
                            var parent = t[0];
                            array.forEach(a, function(c, i) {
                                var child = {id: c, name: c, parent: parent.id};
                                if (i === a.length - 1) {
                                    child.leaf = true;
                                    child.fields = fields;
                                }
                                t.push(child);
                                parent = child;
                            }, this);


                        }
                    }
                    var t2 = array.filter(t, function(item, index, self) {
                        var firstIndex = -1;
                        for (var i = 0, len = self.length; i < len; i++) {
                            if (self[i].id === item.id && self[i].name === item.name && self[i].parent === item.parent) {
                                firstIndex = i;
                                break;
                            }
                        }
                        return firstIndex === index;
                    }, this);

                    var myStore = new Memory({
                        data: t2,
                        getChildren: function(object) {
                            return this.query({parent: object.id});
                        }
                    });

                    // Create the model
                    var myModel = new ObjectStoreModel({
                        store: myStore,
                        query: {id: this.config.name},
                        mayHaveChildren: function(item) {
                            return !item.leaf;
                        }
                    });

                    var tooltip = this.tooltip = new MappingEditorFieldTooltipWidget();

                    

                    // TODO: Workaround for possible dojo bug!
                    popup.moveOffScreen(tooltip.dialog);
                    try {
                        tooltip.startup();
                    } catch (e) {
                    }
                    try {
                        tooltip.dialog.startup();
                    } catch (e) {
                    }
                    // TODO: Workaround for possible dojo bug!
                    
                    
                    

                    this.useAsId = mappings.useAsId;
                    var mw = this;

                    var handlerEnabled, handlerName, handlerAlias, handlerUseAsId;
                    var tree = new Tree({
                        model: myModel,
                        //openOnClick: true,
                        persist: false,
                        style: "width: 50%;",
                        onBlur: function() {
                        },
                        _onBlur: function() {
                        },
                        onClick: function(item, node, event) {

                            popup.close(tooltip.dialog);
                            //console.log(node);
                            if (item.leaf) {
                                event.stopImmediatePropagation();
                                popup.open({
                                    parent: this,
                                    popup: tooltip.dialog,
                                    orient: ["after-centered", "after", "below-alt", "above-alt"],
                                    around: node.domNode,
                                    onExecute: function() {
                                    },
                                    onCancel: function() {
                                    },
                                    onClose: function() {
                                    }
                                });
                                if (handlerEnabled)
                                    handlerEnabled.remove();
                                if (handlerName)
                                    handlerName.remove();
                                if (handlerAlias)
                                    handlerAlias.remove();
                                if (handlerUseAsId)
                                    handlerUseAsId.remove();
                                handlerEnabled = on(dom.byId("popupFieldEnabled"), "change", function(evt) {
                                    array.forEach(item.fields, function(f, i) {
                                        // TODO
                                        // tooltip.enabled.get('checked') returns opposite result in chrome
                                        // maybe fixed in newer dojo
                                        if (evt.target.checked) {
                                            item.fields[i].enabled = true;
                                        }
                                        else {
                                            item.fields[i].enabled = false;
                                        }
                                    }, this);

                                });
                                handlerName = on(dom.byId("popupFieldName"), "change", function() {
                                    array.forEach(item.fields, function(f, i) {
                                        if (item.fields.length > 2) {
                                            item.fields[i].name = tooltip.name.get('value') + f.name.substr(f.name.length - 2);
                                        }
                                        else {
                                            item.fields[0].name = tooltip.name.get('value');
                                        }
                                    }, this);
                                });
                                handlerAlias = on(dom.byId("popupFieldAlias"), "change", function() {
                                    array.forEach(item.fields, function(f, i) {
                                        if (item.fields.length > 2) {
                                            item.fields[i].alias = tooltip.alias.get('value') + f.alias.substr(f.alias.length - 2);
                                        }
                                        else {
                                            item.fields[0].alias = tooltip.alias.get('value');
                                        }
                                    }, this);
                                });
                                handlerUseAsId = on(dom.byId("popupFieldUseAsId"), "change", lang.hitch(mw, function(evt) {
                                    // TODO
                                    // tooltip.enabled.get('checked') returns opposite result in chrome
                                    // maybe fixed in newer dojo
                                    if (evt.target.checked) {
                                        this.useAsId = item.fields[0].name;
                                    }
                                    else {
                                        this.useAsId = "";
                                    }
                                }));

                                node.labelNode.onBlur = function() {
                                };
                                node.labelNode._onBlur = function() {
                                };

                                //domProp.set("popupFieldEnabled", 'checked', item.fields[0].enabled);
                                tooltip.enabled.set('checked', item.fields[0].enabled);
                                if (item.fields.length > 2) {
                                    tooltip.name.set('value', item.fields[0].name.substr(0, item.fields[0].name.length - 2));
                                    tooltip.alias.set('value', item.fields[0].alias.substr(0, item.fields[0].alias.length - 2));
                                }
                                else {
                                    tooltip.name.set('value', item.fields[0].name);
                                    tooltip.alias.set('value', item.fields[0].alias);
                                    if ( mappings.missingGmlId !== undefined 
                                            && (mappings.missingGmlId || !mappings.supportsResIdQuery ) 
                                            && item.fields[0].name !== "gmlId") {
                                        domStyle.set(tooltip.useAsIdRow, "display", "table-row");

                                        if (item.fields[0].name === mw.useAsId) {
                                            tooltip.useAsId.set('checked', true);
                                        }
                                        else {
                                            tooltip.useAsId.set('checked', false);
                                        }
                                    }
                                    else {
                                        domStyle.set(tooltip.useAsIdRow, "display", "none");
                                    }
                                }
                            }
                        },
                        getIconClass: function(item, opened) {
                            return item.leaf ? "dijitLeaf" : (opened ? "dijitFolderOpened" : "dijitFolderClosed");
                        }
                    });
                    tree.placeAt(this.contentArea);
                    tree.startup();
                },
                show: function()
                {
                    lang.hitch(this.dialog, this.dialog.show)();
                },
                hide: function()
                {
                    popup.close(this.tooltip.dialog);
                    this.tooltip.destroyRecursive();
                    lang.hitch(this.dialog, this.dialog.hide)();
                },
                save: function() {
                    if (typeof this.action === 'function') {
                        this.action();
                    }

                    //console.log(this.config.fields);
                    //var service = {id: this.service.id};
                    //service.fullLayers = [this.config];

                    this.parent.mappingEditorChanges = this.config.fieldsConfig;
                    this.parent.useAsId = this.useAsId;
                    //this.destroyProgressMessage = lang.hitch(this.pageWidget, this.pageWidget.progress, "Saving changes for service '" + this.service.id + "'", 0)();
                    //when(this.adminStore.put(service, {incremental: true}), lang.hitch(this, this.onSuccess, service), lang.hitch(this, this.onFailure, service));

                    this.hide();
                }

            });
        });
