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
    "dojo/text!./ServiceEditGeneralWidget/template.html",
    "dojo/_base/lang",
    "dojo/when",
    'dijit/form/Form',
    'dojox/form/manager/_Mixin',
    'dojox/form/manager/_FormMixin',
    'dojox/form/manager/_DisplayMixin',
    'dojox/form/manager/_ValueMixin',
    'dijit/form/ValidationTextBox',
    'dijit/form/Textarea',
    'dojox/validate',
    "dojo/on",
    "xsf/ChildTemplateMixin",
    "dojo/i18n",
    "dojo/string"
],
        function(declare, WidgetBase, TemplatedMixin, WidgetsInTemplateMixin,
                template, lang, when, Form, FormMgrMixin, FormMgrFormMixin,
                FormMgrDisplayMixin, FormMgrValueMixin,
                ValidationTextBox, Textarea, Validate,
                on, ChildTemplateMixin, i18n, string) {
            return declare([Form, WidgetsInTemplateMixin, FormMgrMixin, FormMgrFormMixin, FormMgrDisplayMixin, FormMgrValueMixin, ChildTemplateMixin], {
                templateString: template,
                baseClass: "serviceEditGeneralWidget",
                dialog: null,
                adminStore: null,
                pageWidget: null,
                destroyProgressMessage: null,
                title: "Edit Service",
                service: null,
                constructor: function() {
                    this.messages = i18n.getLocalization("xsf", "Module", navigator.userLanguage || navigator.language);
                     
                    
                     
                },
                postCreate: function() {
                    this.service_name.set("value", this.config.name);                
                    on(this.service_name, "change", lang.hitch(this, this.nameChanged));                 
                },
                nameChanged: function() {
                    this.config.name = this.service_name.get("value");
                }

            });
        });