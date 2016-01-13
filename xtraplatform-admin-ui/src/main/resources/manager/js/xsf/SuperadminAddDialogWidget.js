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
    "dojo/text!./SuperadminAddDialogWidget/template.html",
    "dojo/_base/lang",
    "dojo/when",
    'dijit/form/Form',
    "xsf/FormDialogWidgetMixin",
    "xsf/ChildTemplateMixin",
    "dojo/json",
    "dojo/query",
    "dojo/i18n",
    "dojo/string",
    "dojox/form/PasswordValidator",
    "dijit/form/Select",
    "dojo/dom"
],
        function(declare, WidgetBase, template, lang, when, Form, FormDialogWidgetMixin,
                ChildTemplateMixin, JSON, query, i18n, string, PasswordValidator, Select, dom) {
            return declare([WidgetBase, Form, FormDialogWidgetMixin, ChildTemplateMixin], {
                templateString: template,
                baseClass: "superadminAddDialogWidget",
                dialog: null,
                adminStore: null,
                pageWidget: null,
                destroyProgressMessage: null,
                title: null,
                roleSelect: null,
                postMixInProperties: function()
                {
                    //this.messages = i18n.getLocalization("xsf.userstore", "Module", navigator.userLanguage || navigator.language);

                    this.inherited(arguments);

                },
                postCreate: function() {
                    this.inherited(arguments);
                    this.passwordVerify.validator = this.confirmPassword;
                },
                confirmPassword: function(value, constraints)
                {
                    var isValid = false;
                    if (constraints && constraints.other) {
                        var otherInput = dom.byId(constraints.other);
                        if (otherInput) {
                            var otherValue = otherInput.value;
                            isValid = (value === otherValue);
                        }
                    }
                    return isValid;
                },
                _onSuccess: function(params, response) {
                    this.hide();

                },
                _onFailure: function(params, response) {
                    this.hide();

                },
                onSubmit: function() {

                    this.inherited(arguments);
                    this.validate();

                    if (this.isValid()) {
                        var user = this.gatherFormValues();
                        // TODO: what is the bogus idProperty for ???
                    var userStore = this.adminStore.getSubStore("", {idProperty: "bla"});
                        when(userStore.add(
                                user
                                ), lang.hitch(this, this._onSuccess), lang.hitch(this, this._onFailure));

                        this.hide();
                        this.pageWidget.reload();
                    }

                    return false;
                }
            });
        });