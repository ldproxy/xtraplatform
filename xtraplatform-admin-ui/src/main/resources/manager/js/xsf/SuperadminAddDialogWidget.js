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