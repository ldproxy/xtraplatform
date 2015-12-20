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
    "xsf/SettingsSidebarWidget",
    "xsf/SettingsModulesWidget",
    "xsf/api/SecurityScopeDecider"
],
        function(declare, lang, WidgetBase, dom, domConstruct, array, when, fx,
                FlashMessageMixin, SettingsSidebarWidget, SettingsModulesWidget, SSD) {
            return declare([WidgetBase, FlashMessageMixin], {
                baseClass: "servicesSidebarWidget",
                serviceList: null,
                adminStore: null,
                services: [],
                modules: [],
                settingsSidebar: null,
                buildRendering: function() {
                    this.domNode = domConstruct.create("div");
                    var sidebar = domConstruct.create("div", {
                        id: "sidebar",
                        "class": "left"
                    }, this.domNode);
                    var maincontent = domConstruct.create("div", {
                        id: "maincontent"
                    }, this.domNode);

                    this.flashMessageNode = maincontent;


                    var settingsItems = [];

                    array.forEach(this.modules, function(mod, i) {
                        settingsItems = settingsItems.concat(mod.getSettingsItems());
                    }, this);
                    
                    this.settingsSidebar = new SettingsSidebarWidget({
                        adminStore: this.adminStore,
                        contentContainer: maincontent,
                        settingsItems: settingsItems,
                        pageWidget: this
                    }).placeAt(sidebar);
                },
                refresh: function() {
                    if (this.settingsSidebar && this.settingsSidebar.refresh) {
                        this.settingsSidebar.refresh();
                    }
                }

            });
        });

