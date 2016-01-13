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