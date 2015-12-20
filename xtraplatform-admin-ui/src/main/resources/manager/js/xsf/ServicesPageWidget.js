define([
    "dojo/_base/declare",
    "dojo/_base/lang",
    "dijit/_WidgetBase",
    "dojo/dom",
    "dojo/dom-construct",
    "dojo/_base/array",
    "dojo/when",
    "dojo/_base/fx",
    "xsf/FlashMessageMixin",
    "xsf/ServiceControlWidget",
    "xsf/ServicesSidebarWidget",
    "xsf/api/SecurityScopeDecider",
    "xsf/SuperadminAddDialogWidget",
    "xsf/api/Scope"
],
        function(declare, lang, WidgetBase, dom, domConstruct, array, when, fx,
                FlashMessageMixin, ServiceControlWidget, ServicesSidebarWidget, SSD, SuperadminAddDialogWidget, scope) {
            return declare([WidgetBase, FlashMessageMixin], {
                baseClass: "servicesPageWidget",
                serviceList: null,
                adminStore: null,
                services: [],
                serviceControls: [],
                serviceWidgets: [],
                modules: [],
                addUserDialog: null,
                buildRendering: function() {
                                                            
                    this.domNode = domConstruct.create("div");
                    var maincontent = domConstruct.create("div", {
                        id: "maincontent"
                    }, this.domNode);
                    var sidebar = domConstruct.create("div", {
                        id: "sidebar",
                        "class": "right"
                    }, this.domNode);
                    this.serviceList = domConstruct.create("ul", null, maincontent);

                    this.flashMessageNode = maincontent;

                    // Security
                    var ssd = new SSD({store: this.adminStore});
                    if (ssd.allowed(scope.PUBLISHER)) {
                        new ServicesSidebarWidget({
                            adminStore: this.adminStore,
                            manualProvider: this.manualProvider,
                            pageWidget: this
                        }).placeAt(sidebar);
                    }

                    this.refreshServices();
                },
                refreshServices: function() {
                    this._prepareRendering();
                },
                _prepareRendering: function() {
                    when(this.adminStore.get("services"), lang.hitch(this, this._renderServices));
                },
                _renderServices: function(services) {
                    array.forEach(this.services, function(entry, i) {
                        if (array.indexOf(services, entry) === -1) {
                            this.services.splice(i, 1);
                            domConstruct.destroy(this.serviceControls[i]);
                            this.serviceControls.splice(i, 1);
                        }
                    }, this);

                    array.forEach(services, function(entry, i) {
                        if (array.indexOf(this.services, entry) === -1) {
                            if (!dom.byId("service-" + entry)) {
                                domConstruct.create("li", {
                                    "class": "widget-wrapper",
                                    "id": "service-" + entry
                                }, this.serviceList, "first");
                            }
                        }
                        when(this.adminStore.get("services/" + entry), lang.hitch(this, this._renderService), lang.hitch(this, this._onFailure, entry));
                    }, this);
                },
                _renderService: function(service) {
                    if (array.indexOf(this.services, service.id) === -1) {
                        var item = dom.byId("service-" + service.id);
                        if( item){
                            var widget = new ServiceControlWidget(service, {
                                adminStore: this.adminStore,
                                manualProvider: this.manualProvider,
                                pageWidget: this,
                                modules: this.modules
                            }).placeAt(item);
                            this.services.push(service.id);
                            this.serviceControls.push(item);
                            this.serviceWidgets.push(widget);
                        }
                    }
                },
                _onFailure: function(entry) {
                    when(this.adminStore.get("services/" + entry), lang.hitch(this, this._renderService));
                },
                refresh: function() {
                    domConstruct.empty(this.serviceList);                 
                    this.services = [];
                    array.forEach(this.serviceWidgets, function(entry, i) {
                        entry.destroyRecursive();
                    }, this);
                    this.serviceControls = [];

                    this.refreshServices();
                    this._initFlashMessages();
                }
            });
        });