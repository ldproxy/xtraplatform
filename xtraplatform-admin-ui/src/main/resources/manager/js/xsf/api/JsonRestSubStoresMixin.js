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
