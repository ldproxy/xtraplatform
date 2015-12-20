define([
    "dojo/_base/declare",
    "dojo/_base/lang"
],
        function(declare, lang) {
            return declare(null, {
                getSubStore: function(subTarget, options) {
                    //var subStore = lang.clone(this);
                    var subStore = lang.mixin({}, this);
                    
                    if (subStore.target.charAt(subStore.target.length-1) !== '/' && subTarget.length > 0 && subTarget.charAt(0) !== '/') {
                        subTarget = '/' + subTarget;
                    }
                    else if (subStore.target.charAt(subStore.target.length-1) === '/' && subTarget.length > 0 && subTarget.charAt(0) === '/') {
                        subTarget = subTarget.substr(1);
                    }
                    if (subTarget.length > 0 && subTarget.charAt(subTarget.length-1) !== '/') {
                        subTarget = subTarget + '/';
                    }
                    
                    subStore.target = subStore.target + subTarget;
                    lang.mixin(subStore, options);
                    
                    return subStore;
                }
            });
        });
