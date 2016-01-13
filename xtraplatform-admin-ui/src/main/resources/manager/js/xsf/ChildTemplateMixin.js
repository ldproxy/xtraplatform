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
