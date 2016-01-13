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
    "dijit/Dialog",
    "dojo/query",
    "dojo/dom-class",
    "dojo/dom-construct",
    "dojo/_base/lang",
    "dojo/request",
    'dijit/form/Form',
    'dojox/form/manager/_Mixin', 
    'dojox/form/manager/_NodeMixin', 
    'dojox/form/manager/_FormMixin', 
    'dojox/form/manager/_DisplayMixin',
    'dojox/form/manager/_ValueMixin',
    "dojo/_base/event"
    ],
    function(declare, WidgetBase, TemplatedMixin, WidgetsInTemplateMixin, Dialog, query, domClass, domConstruct, lang, request, Form, 
        FormMgrMixin, FormMgrNodeMixin, FormMgrFormMixin, FormMgrDisplayMixin, FormMgrValueMixin,
        event){
        return declare([Form, WidgetsInTemplateMixin, FormMgrMixin, FormMgrFormMixin, FormMgrNodeMixin, FormMgrDisplayMixin, FormMgrValueMixin], {
            
            templateString: null,
            baseClass: null,
            dialog: null,
            adminStore: null,
            pageWidget: null,
            destroyProgressMessage: null,
            title: "",
            style: "width:760px; max-height: 90%; overflow:hidden;",
            
            postMixInProperties: function()
            {
                this.dialog = new Dialog({
                    title: this.title, 
                    style: this.style,
                    parseOnLoad: false,
                    draggable: false,
                    doLayout: false
                });
                
                query(".closeText", this.dialog.domNode).addClass("icon-remove-circle").empty();
                                
                //query(".dijitDialogTitleBar", this.addServiceDialog.domNode).addClass("dijitButtonNode");
                //domClass.add(this.addServiceDialog.domNode, "inverse");
                                
                this.inherited(arguments);
            },
 
            postCreate: function()
            {
                this.dialog.set("content", this.domNode);
                
                this.dialog.onCancel = lang.hitch(this, this.hide);
                this.cancelButton.onClick = lang.hitch(this, this.hide);
                
                this.inherited(arguments);
            },
 
            show: function()
            {
                lang.hitch(this.dialog, this.dialog.show)();
            },
 
            hide: function()
            {
                lang.hitch(this.dialog, this.dialog.hide)();
                this.reset();
                this.onHide();
            },
            
            onHide: function() {                
            },
            
            // we override this because the implementation
            // in _FormMixin.js causes an endless loop in IE
            _onReset: function(evt){
                 event.stop(evt);
		 return false;
            },
            
            onSubmit:function () {
                this.inherited(arguments);
                //this.validate();
                return false;
            }
            
        });
    });
