define([
    "dojo/_base/declare",
    "dijit/_WidgetBase", 
    "dojo/dom-construct"
    ],
    function(declare, WidgetBase, domConstruct){
        return declare([WidgetBase], {
            
            _beforeFillContent: function(){
                var tmpl = this._getChildFormTemplate();
                if(tmpl && this.formElementList && this.ownerDocument) {
                    domConstruct.place(domConstruct.toDom(this._stringRepl(tmpl), this.ownerDocument), this.formElementList);
                }
                this.inherited(arguments);
            },
            
            _getChildFormTemplate: function() {                
            }
            
        });
    });
