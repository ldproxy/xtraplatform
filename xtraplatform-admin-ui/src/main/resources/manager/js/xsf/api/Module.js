define([
    "dojo/_base/declare"
],
        function(declare) {
            return declare([], {
                
                getMainMenuItems: function() {
                    return [];
                },
                        
                getTopMenuItems: function() {
                    return [];
                },
                        
                getRightFooterItems: function() {
                    return [];
                },
                        
                getSettingsItems: function() {
                    return [];
                },
                
                getSecurityItems: function() {
                    return [];
                },
                
                getLogItems: function() {
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
                },
                
                applyStoreMixins: function(store) {
                    return store;
                },
                
                getManualProvider: function() {
                    return null;
                }
            });
        });