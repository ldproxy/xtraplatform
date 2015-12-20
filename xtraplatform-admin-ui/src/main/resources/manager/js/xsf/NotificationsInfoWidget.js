define([
    "dojo/_base/declare",
    "dijit/_WidgetBase",
    "dijit/_TemplatedMixin",
    "dijit/_WidgetsInTemplateMixin",
    "dojo/text!./NotificationsInfoWidget/template.html",
    "xsf/FormDialogWidgetMixin",
    "dojo/_base/lang",
    "dojo/when",
    'dijit/form/Form',
    'dojox/form/manager/_Mixin',
    'dojox/form/manager/_FormMixin',
    'dojox/form/manager/_DisplayMixin',
    'dojox/form/manager/_ValueMixin',
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
                on, ChildTemplateMixin, Source, domConstruct, i18n, string, array, domStyle) {
            return declare([Form, WidgetsInTemplateMixin, FormMgrMixin,
                FormMgrFormMixin, FormMgrDisplayMixin, FormMgrValueMixin,
                ChildTemplateMixin, FormDialogWidgetMixin], {
                templateString: template,
                baseClass: "NotificationsInfoWidget",
                dialog: null,
                adminStore: null,
                pageWidget: null,
                destroyProgressMessage: null,
                title: "Info",
                notifications: null,
                service: null,
                groups: null,
                groupsAllowed: null,
                constructor: function() {
                    this.inherited(arguments);
                    this.messages = i18n.getLocalization("xsf", "Module", navigator.userLanguage || navigator.language);
                },
                postCreate: function() {
                    this.inherited(arguments);
                                        
                    array.forEach(this.notifications, function(entry, i) {
                            var item = domConstruct.create("li", {}, this.formElementList);
                            
                            var title = domConstruct.create("div", {style: "font-weight: bold;"}, item);
                            title.innerHTML = entry.level;
                            
                            var span = domConstruct.create("div", {style: "margin-right: 20px;"}, item);
                            span.innerHTML = entry.message;
                            
                            domConstruct.create("div", {style: "height: 20px;"}, item);
                    }, this);
                },
                show: function() {
                    this.inherited(arguments);
                }
            });
        });