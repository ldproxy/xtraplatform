define([
    "dojo/_base/declare",
    "xsf/api/Module",
    "xsf/devinfo/DevInfoWidget"
],
        function(declare, Module, DevInfoWidget) {
            return declare([Module], {
                getMainMenuItems: function() {
                    return [];
                },
                getTopMenuItems: function() {
                    return [];
                },
                getRightFooterItems: function() {
                    return [
                        {
                            id: "devinfo",
                            widget: DevInfoWidget
                        }
                    ];
                },  
                getSettingsItems: function() {
                    return [];
                },
                getServiceSettingsItems: function() {
                    return [];
                },
                getServiceViews: function() {
                    return [];
                },
                getServiceActions: function() {
                    return [];
                }
            });
        });