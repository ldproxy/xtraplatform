define([
    "dojo/_base/declare",
    "dijit/_WidgetBase", 
    "dijit/_TemplatedMixin",
    "dijit/_WidgetsInTemplateMixin",
    "dojo/text!./MappingEditorFieldTooltipWidget/template.html",
    "dijit/TooltipDialog",
    "dojo/_base/lang",
    'dijit/form/Form',
    'dojox/form/manager/_Mixin', 
    'dojox/form/manager/_FormMixin', 
    "dijit/form/TextBox", 
    "dijit/form/CheckBox"
    ],
    function(declare, WidgetBase, TemplatedMixin, WidgetsInTemplateMixin, template, TooltipDialog, lang, Form, FormMgrMixin, FormMgrFormMixin){
        return declare([Form, WidgetsInTemplateMixin, FormMgrMixin, FormMgrFormMixin], {
            
            templateString: template,
            dialog: null,
                       
            postMixInProperties: function()
            {
                this.dialog = new TooltipDialog();
                
                this.inherited(arguments);
            },
 
            postCreate: function()
            {
                this.dialog.set("content", this.domNode);
                
                this.inherited(arguments);
            }
            
        });
    });
