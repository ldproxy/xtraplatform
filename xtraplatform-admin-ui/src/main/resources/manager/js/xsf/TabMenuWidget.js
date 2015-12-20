define([
    "dojo/_base/declare",
    "dojo/_base/lang",
    "dijit/_WidgetBase",
    "dojo/dom",
    "dojo/dom-construct",
    "dojo/dom-prop",
    "dojo/_base/array",
    "dojo/on",
    "dojo/dom-class",
    "dojo/window",
    "dojo/request"
],
        function(declare, lang, WidgetBase, dom, domConstruct, domProp, array, on, domClass, win, request) {
            return declare([WidgetBase], {
                tabItems: null,
                widgetParams: null,
                contentContainer: null,
                initAll: false,
                autoStart: true,
                selectedItem: null,
                buildRendering: function() {
                    array.forEach(this.tabItems, function(entry, i) {
                        this.createTabDom(entry);
                    }, this);
                    if (this.autoStart) {
                        this.start();
                    }
                },
                start: function() {
                    
                    array.forEach(this.tabItems, function(entry, i) {
                        if (entry.selected) {
                            domClass.add(entry.node, "selected");
                            if (entry.widget) {
                                entry.widgetInstance = new entry.widget(this.widgetParams).placeAt(this.contentContainer);
                            }
                            else if (entry.partial) {
                                var update = lang.hitch(this, function(response) {
                                    domProp.set(this.contentContainer, 'innerHTML', response);
                                });
                                request.get(entry.partial).then(update);
                            }
                            this.selectedItem = entry;
                            
                            if( entry.allwaysRefresh ) {
                            if( entry.widgetInstance.refresh ) {
                                entry.widgetInstance.refresh();
                            }
                        }
                        }
                        else if (this.initAll && entry.widget) {
                            entry.widgetInstance = new entry.widget(this.widgetParams);
                        }
                    }, this);
                },
                addTab: function(entry) {
                    this.createTabDom(entry);
                    this.tabItems.push(entry);
                },
                createTabDom: function(entry) {
                    entry.node = domConstruct.create("li", null, this.domNode);
                    domConstruct.create("span", {
                        innerHTML: entry.label
                    }, entry.node);
                    on(entry.node, "click", lang.hitch(this, this._selectTab, entry));
                },
                _selectTab: function(item, event) {
                    event.preventDefault();
                    if (!item.selected) {
                        if (this.selectedItem && this.selectedItem.widgetInstance) {
                            this.contentContainer.removeChild(this.selectedItem.widgetInstance.domNode);
                        }
                        else if (this.selectedItem && this.selectedItem.partial) {
                            domProp.set(this.contentContainer, 'innerHTML', '');
                        }
                        array.forEach(this.tabItems, function(entry, i) {
                            if (item == entry) {
                                entry.selected = true;
                                domClass.add(entry.node, "selected");
                            }
                            else if( entry.selected) {
                                entry.selected = false;
                                domClass.remove(entry.node, "selected");
                            }
                        }, this);
                        if (item.widget) {
                            if (!item.widgetInstance ) {
                                item.widgetInstance = new item.widget(this.widgetParams);
                            }
                            
                            item.widgetInstance.placeAt(this.contentContainer);
                            item.widgetInstance.startup();
                            
                            if( item.allwaysRefresh ) {
                                if( item.widgetInstance.refresh ) {
                                    item.widgetInstance.refresh();
                                }
                            }
                        }
                        else if (item.partial) {
                            var update = lang.hitch(this, function(response) {
                                domProp.set(this.contentContainer, 'innerHTML', response);
                            });
                            request.get(item.partial).then(update);
                        }
                        this.selectedItem = item;
                    }

                    win.scrollIntoView(this.contentContainer);

                    return false;
                },
                refresh: function() {
                    if (this.selectedItem && this.selectedItem.widgetInstance && this.selectedItem.widgetInstance.refresh) {
                        this.selectedItem.widgetInstance.refresh();
                    }
                }
            });
        });