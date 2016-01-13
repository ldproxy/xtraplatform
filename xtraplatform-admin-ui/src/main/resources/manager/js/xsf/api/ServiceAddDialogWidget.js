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
    "dojo/text!./ServiceAddDialogWidget/template.html",
    "dojo/_base/lang",
    "dojo/when",
    'dijit/form/Form',
    "xsf/FormDialogWidgetMixin",
    "xsf/ChildTemplateMixin",
    "dojo/json",
    "dojo/query",
    "dojo/i18n",
    "dojo/string"
],
        function(declare, WidgetBase, template, lang, when, Form, FormDialogWidgetMixin,
                ChildTemplateMixin, JSON, query, i18n, string) {
            return declare([WidgetBase, Form, FormDialogWidgetMixin, ChildTemplateMixin], {
                templateString: template,
                baseClass: "serviceAddDialogWidget",
                dialog: null,
                adminStore: null,
                pageWidget: null,
                destroyProgressMessage: null,
                title: null,
                postMixInProperties: function()
                {
                    this.messages = i18n.getLocalization("xsf.api", "Module", navigator.userLanguage || navigator.language); 
                    this.inherited(arguments);
                    
                    if (this.manualProvider) {
                        this.manualProvider.getManualIcon( this.dialog.titleBar, "serviceadd");
                    }
                },
                onSuccess: function(params, response) {
                    this.destroyProgressMessage();
                    this.hide();
                    lang.hitch(this.pageWidget, this.pageWidget.success,
                            dojo.string.substitute(this.messages.createdServiceSuccess, {type: params.type, id: params.id}), 5000)();
                    lang.hitch(this.pageWidget, this.pageWidget.refreshServices)();
                },
                onFailure: function(params, response) {
                    this.destroyProgressMessage();
                    this.hide();

                    var error = JSON.parse(response.response.data);

                    var details = "<ul>";
                    for (var i in error.error.details) {
                        details = details + "<li>" + error.error.details[i] + "<br/></li>";
                    }
                    details = details + "</ul>";

                    lang.hitch(this.pageWidget, this.pageWidget.error,
                            dojo.string.substitute(this.messages.createdServiceFail, {type: params.type, id: params.id, details: details}), 0)();
                },
                onSubmit: function() {
                    this.inherited(arguments);
                    this.validate();
                    if (this.isValid()) {
                        var params = this.gatherFormValues();
                        lang.hitch(this.dialog, this.dialog.hide)();
                        this.destroyProgressMessage = lang.hitch(this.pageWidget, this.pageWidget.progress,
                                dojo.string.substitute(this.messages.creatingService, {type: params.type, id: params.id}), 0)();
                        when(this.adminStore.add(params), lang.hitch(this, this.onSuccess, params), lang.hitch(this, this.onFailure, params));

                    }
                    return false;
                },
                setServiceType: function(type) {
                    this.serviceType.value = type;
                    this.dialog.titleNode.innerHTML = dojo.string.substitute(this.messages.addTypeService, {type: this.serviceType.value});
                }

            });
        });