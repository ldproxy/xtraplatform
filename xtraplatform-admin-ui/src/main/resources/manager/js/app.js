define([
    "dojo/_base/lang",
    "dojo/dom",
    "dojo/store/JsonRest",
    "dojo/_base/array",
    "dojo/when",
    "xsf/TabMenuWidget",
    "xsf/Module",
    "xsf/api/SecurityScopeDecider",
    "require"
],
        function (lang, dom, JsonRest, array, when, TabMenuWidget, Core, SSD, require) {
            var adminStore = new JsonRest({
                target: "../rest/admin/",
                idProperty: "id"
            }),
                    mainMenu = null,
                    topMenu = null,
                    rightFooter = null,
                    widgetParams = null,
                    moduleCount = 0,
                    orgid = null,
                    manualProvider = null,
                    _compareItems = function (a, b) {
                        if (a.ordinal === undefined)
                            a.ordinal = Infinity;

                        if (b.ordinal === undefined)
                            b.ordinal = Infinity;

                        if (a.ordinal < b.ordinal)
                            return -1;
                        if (a.ordinal > b.ordinal)
                            return 1;
                        return 0;
                    },
                    initUi = function (org_id, modules) {
                        moduleCount = modules.length;
                        orgid = org_id;
                        var core = new Core();

                        widgetParams = {
                            adminStore: core.applyStoreMixins(adminStore),
                            modules: [core]
                        };

                        mainMenu = {
                            tabItems: core.getMainMenuItems(),
                            domNode: dom.byId("menu"),
                            contentContainer: dom.byId("content"),
                            autoStart: moduleCount === 0
                        };

                        topMenu = {
                            items: core.getTopMenuItems(),
                            domNode: dom.byId("top-menu")
                        };

                        rightFooter = {
                            items: core.getRightFooterItems(),
                            domNode: dom.byId("right-footer")
                        };

                        array.forEach(modules, function (entry, i) {
                            require(['xsf/' + entry + "/Module"],
                                    function (Module) {
                                        var mod = new Module();
                                        if (mod.getManualProvider()) {
                                            this.manualProvider = mod.getManualProvider();
                                        }
                                    });
                        }, this);


                        array.forEach(modules, function (entry, i) {
                            require(['xsf/' + entry + "/Module"],
                                    function (Module) {
                                        var mod = new Module();
                                        widgetParams.adminStore = mod.applyStoreMixins(widgetParams.adminStore);
                                        
                                        if (this.manualProvider) {
                                            widgetParams.manualProvider = this.manualProvider;
                                        }
                                        
                                        widgetParams.modules.push(mod);
                                        array.forEach(mod.getMainMenuItems(), function (item, i) {
                                            mainMenu.tabItems.push(item);
                                        }, this);
                                        array.forEach(mod.getTopMenuItems(), function (item, i) {
                                            topMenu.items.push(item);
                                        }, this);
                                        array.forEach(mod.getRightFooterItems(), function (item, i) {
                                            rightFooter.items.push(item);
                                        }, this);

                                        moduleLoaded();
                                    });
                        }, this);
                    },
                    moduleLoaded = function () {
                        moduleCount--;
                        if (moduleCount === 0) {

                            if (widgetParams.adminStore._isAuthorized) {
                                if (orgid) {
                                    widgetParams.adminStore.token_id = orgid;
                                }
                                when(widgetParams.adminStore._isAuthorized(), start);
                            }
                            else {
                                start(true);
                            }
                        }
                    },
                    start = function (authorized) {
                        if (!authorized) {
                            widgetParams.adminStore.authorize();
                        }

                        // filter main menu
                        /*array.forEach(mainMenu.tabItems, function(entry, i) {
                         if (entry && widgetParams.adminStore.token_id === entry.forbidden) {
                         mainMenu.tabItems.splice(i, 1);
                         }
                         }, this);*/


                        // Security
                        var ssd = new SSD({store: widgetParams.adminStore});
                        mainMenu.tabItems = ssd.filterMainMenuItems(mainMenu.tabItems);

                        mainMenu.tabItems.sort(_compareItems);
                        if (mainMenu.tabItems[0]) {
                            mainMenu.tabItems[0].selected = true;
                        }

                        mainMenu.widgetParams = widgetParams;
                        var mainMenuWidget = new TabMenuWidget(mainMenu);
                        mainMenuWidget.widgetParams = widgetParams;
                        mainMenuWidget.start();

                        topMenu.items.sort(_compareItems);
                        array.forEach(topMenu.items, function (item, i) {
                            new item.widget(widgetParams).placeAt(topMenu.domNode);
                        }, this);

                        rightFooter.items.sort(_compareItems);
                        array.forEach(rightFooter.items, function (item, i) {
                            new item.widget(widgetParams).placeAt(rightFooter.domNode);
                        }, this);
                    };
            return {
                init: function (orgid) {

                    // proceed directly with startup
                    when(adminStore.get("modules"), lang.hitch(this, initUi, orgid ? orgid : null));
                }
            };
        });