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