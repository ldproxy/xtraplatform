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
    "dojo/text!./ServiceEditSecurityWidget/template.html",
    "xsf/FormDialogWidgetMixin",
    "dojo/_base/lang",
    "dojo/when",
    'dijit/form/Form',
    'dojox/form/manager/_Mixin',
    'dojox/form/manager/_FormMixin',
    'dojox/form/manager/_DisplayMixin',
    'dojox/form/manager/_ValueMixin',
    'dijit/form/ValidationTextBox',
    'dijit/form/RadioButton',
    'dijit/form/Textarea',
    'dojox/validate',
    "dojo/on",
    "xsf/ChildTemplateMixin",
    "dojo/dnd/Source",
    "dojo/dom-construct",
    "dojo/i18n",
    "dojo/string",
    "dojo/_base/array",
    "dojo/dom-style"
],
        function(declare, WidgetBase, TemplatedMixin, WidgetsInTemplateMixin,
                template, FormDialogWidgetMixin, lang, when, Form, FormMgrMixin, FormMgrFormMixin,
                FormMgrDisplayMixin, FormMgrValueMixin,
                ValidationTextBox, RadioButton, Textarea, Validate,
                on, ChildTemplateMixin, Source, domConstruct, i18n, string, array, domStyle) {
            return declare([Form, WidgetsInTemplateMixin, FormMgrMixin,
                FormMgrFormMixin, FormMgrDisplayMixin, FormMgrValueMixin,
                ChildTemplateMixin, FormDialogWidgetMixin], {
                templateString: template,
                baseClass: "serviceEditSecurityWidget",
                dialog: null,
                adminStore: null,
                pageWidget: null,
                destroyProgressMessage: null,
                title: "Edit Service Security",
                groupslist: [],
                settings: null,
                service: null,
                groups: null,
                groupsAllowed: null,
                constructor: function() {
                    this.inherited(arguments);
                    this.messages = i18n.getLocalization("xsf", "Module", navigator.userLanguage || navigator.language);
                },
                postCreate: function() {
                    this.inherited(arguments);
                    
                    if (this.manualProvider) {
                        this.manualProvider.getManualIcon( this.dialog.titleBar, "servicesecurityedit");
                    }

                    var filteredgroups = lang.clone(this.groupslist);
                    array.forEach(this.settings.groups, function(item, index) {
                        array.forEach(filteredgroups, function(item1, index1) {
                            if (item === item1) {
                                filteredgroups.splice(index1, 1);
                            }

                        });
                    });

                    // create the data items
                    var groupsData = [];
                    var groupsAllowedData = [];

                    array.forEach(filteredgroups, function(item, index) {
                        groupsData.push( { data: item, id: item } );
                    });

                    array.forEach(this.settings.groups, function(item, index) {
                        groupsAllowedData.push( { data: item, id: item } );
                    });

                    // style the item
                    function nodeCreator(item, hint) {
                        var node = domConstruct.create("div");
                        domConstruct.create("span", {class: "icon-group", style: "margin-right: 5px;"}, node);
                        domConstruct.create("span", {innerHTML: item.data}, node);
                        return {node: node, data: item};
                    }

                    // create the Sources
                    this.groups = new Source("groupsNode", {creator: nodeCreator});
                    groupsData.sort();
                    this.groups.insertNodes(false, groupsData);

                    this.groupsAllowed = new Source("groupsAllowedNode", {creator: nodeCreator});
                    groupsAllowedData.sort();
                    this.groupsAllowed.insertNodes(false, groupsAllowedData);

                    if (this.settings.obligation === "PUBLIC" || this.settings.obligation === undefined) {
                        this.public.set('checked', true);
                    } else if (this.settings.obligation === "PRIVATE") {
                        this.private.set('checked', true);
                    } else if (this.settings.obligation === "PRIVATE_WITH_GROUPS") {
                        this.privategroups.set('checked', true);
                    }

                    on(this.public, "change", lang.hitch(this, this._valueChangedPublic));
                    on(this.private, "change", lang.hitch(this, this._valueChangedPrivate));
                    on(this.privategroups, "change", lang.hitch(this, this._valueChangedPrivateGroups));
                },
                show: function() {
                    this.inherited(arguments);
                    domStyle.set(this.domNode, "height", "");
                },
                _valueChangedPublic: function() {
                    if (this.public.checked) {
                        this.disableNode(this.groups.node);
                        this.disableNode(this.groupsAllowed.node);
                        this.disableNode(this.groupsNode_li);
                        this.disableNode(this.groupsAllowedNode_li);
                    }
                },
                _valueChangedPrivate: function() {
                    if (this.private.checked) {
                        this.disableNode(this.groups.node);
                        this.disableNode(this.groupsAllowed.node);
                        this.disableNode(this.groupsNode_li);
                        this.disableNode(this.groupsAllowedNode_li);
                    }
                },
                _valueChangedPrivateGroups: function() {
                    if (this.privategroups.checked) {
                        this.enableNode(this.groups.node);
                        this.enableNode(this.groupsAllowed.node);
                        this.enableNode(this.groupsNode_li);
                        this.enableNode(this.groupsAllowedNode_li);
                    }
                },
                _onSuccess: function(params, response) {
                    this.hide();
                },
                _onFailure: function(params, response) {
                    this.hide();

                },
                onSubmit: function() {
                    var groupsarray = [];                   
                    this.groupsAllowed.forInItems(function(f, o) {
                        groupsarray.push(f.data.id);
                    });

                    var settings = this.gatherFormValues();
                    settings.groups = groupsarray;

                    settings.resource = "services-" + this.service.id;
                    if (settings.setting === "public") {
                        settings.obligation = "PUBLIC";
                    } else if (settings.setting === "private" /*&& settings.privategroups === false*/) {
                        settings.obligation = "PRIVATE";
                    } else if (settings.setting === "privategroups" /*&& settings.privategroups === true*/) {
                        settings.obligation = "PRIVATE_WITH_GROUPS";
                    }

                    var adminStore = this.adminStore.getSubStore("../permissions/services-" + this.service.id);
                    when(adminStore.put(
                            settings, {incremental: true}
                    ), lang.hitch(this, this._onSuccess), lang.hitch(this, this._onFailure));

                    return false;
                },
                hideNode: function(node) {
                    domStyle.set(node, 'display', 'none');
                },
                showNode: function(node) {
                    domStyle.set(node, 'display', '');
                },
                disableNode: function(node) {
                    domStyle.set(node, 'display', 'none');
                    //node.disabled = 'disabled';
                },
                enableNode: function(node) {
                    domStyle.set(node, 'display', '');
                    //node.disabled = '';
                },
            });
        });