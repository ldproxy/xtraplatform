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
    "dojo/text!./ConfirmationDialogWidget/template.html",
    "dijit/Dialog",
    "dojo/query",
    "dojo/dom-construct",
    "dojo/_base/lang",
    'dijit/form/Form',
    'dojox/form/manager/_Mixin', 
    'dojox/form/manager/_FormMixin', 
    "dijit/form/Button", 
    "dijit/form/Select",
    "dojo/i18n",
    "dojo/string"
    ],
    function(declare, WidgetBase, TemplatedMixin, WidgetsInTemplateMixin, 
    template, Dialog, query, domConstruct, lang, Form, 
    FormMgrMixin, FormMgrFormMixin,Button, Select, i18n, string){
        return declare([Form, WidgetsInTemplateMixin, FormMgrMixin, FormMgrFormMixin], {
            
            templateString: template,
            dialog: null,
            title: "",
            content: "",
            action: null,
            style: "width:760px;",
                       
            postMixInProperties: function()
            {
                this.messages = i18n.getLocalization("xsf", "Module", navigator.userLanguage || navigator.language);
                
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
                //this.reset();
                this.onHide();
            },
            
            onHide: function() {                
            },
            
            onSubmit:function () {
                this.inherited(arguments);
                if (typeof this.action === 'function') {
                    this.action();
                }
                this.hide();
                
                return false;
            }
            
        });
    });
