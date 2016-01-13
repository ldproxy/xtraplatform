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
    "dojo/ready",
    "dojo/_base/declare",
    "dojo/_base/lang",
    "dijit/_WidgetBase", 
    "dojo/dom", 
    "dojo/dom-construct", 
    "dojo/on",
    "dojo/_base/array", 
    "dojo/when",
    "dojo/_base/fx",
    "dijit/form/Button",
    "dijit/Dialog",
    "dijit/form/Select", 
    "dijit/form/DropDownButton", 
    "dojo/dnd/Source",
    "dojo/store/JsonRest", 
    "dojo/NodeList-dom"
    
],
function(ready, declare, lang, WidgetBase,  dom, domConstruct, on, array, when, fx, Button,Dialog,Select,DropDownButton,Source ){
	
		
	
    ready(function(){
          
    });
	
    return declare([WidgetBase], {
	
        baseClass: "securityPageWidget",
        serviceList : null,
        securityStore: null,
        services: [],
        addServiceButton: null,
			
        groupSelect: null,
			 
           		         
            
        buildRendering: function(){
			
            this.domNode = domConstruct.create("div");
				
            this.usersingroup = [];
			
				
               
            maincontent = domConstruct.create("div", {
                id: "securityPageWidget"
            }, this.domNode);
				
            cont = domConstruct.create("div", {
                id: "cont",
                class: "cont"
            }, this.domNode);
				
            users = domConstruct.create("div", {
                id: "users",
                class: "secContainer"
            }, cont);
								
            userNode = domConstruct.create("div", {
                id: "userNode",
                class: "dndItem"
            }, users);
				
            users = domConstruct.create("div", {
                id: "group",
                class: "secContainer"
            }, cont);
				
            groupNode = domConstruct.create("div", {
                id: "groupNode",
                class: "dndItem"
            }, users);
				
				
				
            this.groupSelect = new Select({
                name: "groupSelect",
                id: "groupSelect",
                class: "select",
                onChange: lang.hitch(this, this.groupChanged)
            } ).placeAt(this.domNode);
                
                
            var myButton = new Button({
                label: "Save",
                class: "button",
                onClick: lang.hitch(this, this.saveUsers)
            }).placeAt(this.domNode);
		                
            this.refreshServices();
                
        },
            
        saveUsers: function() {
            console.log("save: ");           
            var items = this.getAllItems( this.groupNode);
            array.forEach(items, function(entry, i){ 
                console.log( entry.data);
            }, this.domNode);
        },
        			
        groupChanged: function() {   
            console.log("Selection changed ...");
            var el = this.groupSelect;
            var idx = 0;
            array.forEach(el.options, function(entry, i){ 
                if( entry.selected == true) {
                    idx = entry.value;
                }		
            }, this.domNode);
				
            console.log("-> ", idx);
			
            when(this.securityStore.get("groups/"+idx), lang.hitch(this, this.loadUsersInGroup));
			   
        },
			
        loadUsersInGroup: function(users) {
			
            this.usersingroup = users.users;
				
            this.groupNode.selectAll().deleteSelectedNodes();	
						
            this.groupNode.insertNodes(false, users.users);
				
            when(this.securityStore.get("users"), lang.hitch(this, this._renderUsers));
        },
			
			            
        refreshServices: function() {
            when(this.securityStore.get("users"), lang.hitch(this, this._renderUsers));
            when(this.securityStore.get("groups"), lang.hitch(this, this._renderGroups));
                
            this.groupChanged();
        },
            
        _renderGroups: function(groups) {
		 	 
            array.forEach(groups.groups, function(entry, i){ 
				
                this.groupSelect.addOption({
                    disabled:false,
                    label:entry,
                    selected:true,
                    value:i
                });
            }, this);
			 
        },
			
        _renderUsers: function(users) {
                
            if( !this.userNode) {
                this.userNode =
                    new Source("userNode");
				
                this.groupNode =
                    new Source("groupNode");
            }
						
            this.userNode.selectAll().deleteSelectedNodes();
				
            var a2 = [];
            var add = true;
            array.forEach(users.users, function(entry, i){ 
				
                add = true;
                array.forEach( this.usersingroup, function(entry2, i){ 
                    if( entry === entry2) {
                        add = false;
                    }
                }, this);
					
                if( add) {
                    a2.push( entry);
                }
				
            }, this);
						
            this.userNode.insertNodes(false, a2);
                							                
        },            
            
         getAllItems: function(source){
            var items = source.getAllNodes().map(function(node){
                return source.getItem(node.id);
            });
            return items;
        }
       
    });
});