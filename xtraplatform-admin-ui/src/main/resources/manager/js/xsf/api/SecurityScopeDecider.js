define([
    "dojo/_base/declare",
    "dojo/_base/lang",
    "dojo/when",
    "dojo/_base/array",
    "xsf/api/Scope"
],
        function(declare, lang, when, array, scopes) {
            return declare([], {
                store: null,
                constructor: function(args) {
                    declare.safeMixin(this, args);
                },
                allowed: function(scope, forbidden) {      
                    if (this.store.oauthTokenInfo) {
                        
                        if(this.store.token_id === forbidden) {
                            return false;
                        }
                        
                        var thisScope = this.store.getScope();
                        if( !thisScope){
                            thisScope = "USER";
                        }
                        if( !scope) {
                            scope = 0;
                        }
                        var userscope;
                        if (thisScope === "USER") {
                            userscope = scopes.USER;
                        } else if (thisScope === "PUBLISHER") {
                            userscope = scopes.PUBLISHER;
                        } else if (thisScope === "ADMINISTRATOR") {
                            userscope = scopes.ADMINISTRATOR;
                        } else if (thisScope === "SUPERADMINISTRATOR") {
                            userscope = scopes.SUPERADMINISTRATOR;
                        }
                        //console.log(userscope + " >= " + scope);
                        if( userscope >= scope  ) {
                            return true;
                        }
                    } else {
                        return true;
                    }
                    return false;
                },
                _allowed: function(item) {
                    return this.allowed(item.scope, item.forbidden);
                },
                filterMainMenuItems: function(nodes) {
                    return array.filter(nodes, this._allowed, this );
                }
            });
        });