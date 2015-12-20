define([
    "dojo/_base/declare",
    "dojo/_base/lang",
    "dijit/_WidgetBase",
    "dijit/_TemplatedMixin",
    "dojo/text!./DevInfoWidget/template.html",
    "dojo/query",
    "dojo/request",
    "dojo/json"
],
        function(declare, lang, WidgetBase, TemplatedMixin, template, query, request, JSON) {
            return declare([WidgetBase, TemplatedMixin], {
                templateString: template,
                baseClass: "devInfoWidget",
                bundlesTotal: 0,
                bundlesActive: 0,
                ipojosTotal: 0,
                ipojosActive: 0,
                memoryUsed: '',
                postMixInProperties: function()
                {
                    this.inherited(arguments);
                },
                postCreate: function() {
                    // static badges
                    query(".badge", this.domNode).style('fontFamily', 'DejaVu Sans, Verdana, Geneva, sans-serif').style('fontSize', '11px').style('color', 'white');
                    query(".badge.tag", this.domNode).style('backgroundColor', '#4c4c4c').style('padding', '2px 4px 3px 7px').style('borderRadius', '4px 0 0 4px').style('marginLeft', '5px');
                    query(".badge.value", this.domNode).style('padding', '2px 7px 3px 4px').style('borderRadius', '0 4px 4px 0');
                    query(".badge.stage", this.domNode).style('backgroundColor', '#c72');
                    query(".badge.svn", this.domNode).style('backgroundColor', '#487');
                    query(".badge.revision", this.domNode).style('backgroundColor', '#29c');
                    query(".badge.bundles", this.domNode).style('backgroundColor', '#4c4c4c');
                    query(".badge.ipojos", this.domNode).style('backgroundColor', '#4c4c4c');
                    query(".badge.memory", this.domNode).style('backgroundColor', '#17b');
                    
                    
                    // /system/console/bundles.json
                    // .s[1]/.s[0]
                    // /system/console/iPOJO/instances.json
                    // .valid_count/.count
                    // /system/console/memoryusage
                    // var __pools__ = [{'name':'Code Cache','type':'Non-heap memory','used':'3.61MB','init':'163.84kB','committed':'3.68MB','max':'33.56MB','score':'10%'},{'name':'Eden Space','type':'Heap memory','used':'147.16kB','init':'4.53MB','committed':'8.72MB','max':'71.64MB','score':'0%'},{'name':'Survivor Space','type':'Heap memory','used':'983.16kB','init':'524.29kB','committed':'1.05MB','max':'8.92MB','score':'11%'},{'name':'Tenured Gen','type':'Heap memory','used':'16.44MB','init':'11.21MB','committed':'21.50MB','max':'178.98MB','score':'9%'},{'name':'Perm Gen','type':'Non-heap memory','used':'18.21MB','init':'12.59MB','committed':'18.36MB','max':'67.11MB','score':'27%'},{'name':'Perm Gen [shared-ro]','type':'Non-heap memory','used':'4.75MB','init':'10.49MB','committed':'10.49MB','max':'10.49MB','score':'45%'},{'name':'Perm Gen [shared-rw]','type':'Non-heap memory','used':'6.86MB','init':'12.59MB','committed':'12.59MB','max':'12.59MB','score':'54%'},{'name':'Total','type':'TOTAL','used':'50.98MB','init':'52.07MB','committed':'76.35MB','max':'383.26MB','score':'13%'}];
                    
                    request("../system/console/bundles.json", {handleAs: 'json'}).then(lang.hitch(this, this.refreshBundlesBadge));
                    request("../system/console/iPOJO/instances.json", {handleAs: 'json'}).then(lang.hitch(this, this.refreshIpojosBadge));
                    request("../system/console/memoryusage").then(lang.hitch(this, this.refreshMemoryBadge));
                    

                    // light blue #29c
                    // blue #17b
                    // dark green #487
                    // green #4b2
                    // red #c54
                    // orange #c72




                    this.inherited(arguments);
                },
                refreshBundlesBadge: function(data) {
                    this.bundlesTotal = data.s[0];
                    this.bundlesActive = data.s[1];
                    
                    if (this.bundlesActive < this.bundlesTotal) {
                        query(".badge.bundles", this.domNode).addClass('failure').addContent(this.bundlesActive + '/' + this.bundlesTotal);
                    }
                    else {
                        query(".badge.bundles", this.domNode).addClass('success').addContent(this.bundlesActive + '/' + this.bundlesTotal);
                    }
                    this.refreshDynamicBadges();
                },
                refreshIpojosBadge: function(data) {
                    this.ipojosTotal = data.count;
                    this.ipojosActive = data.valid_count;
                    
                    if (this.ipojosActive < this.ipojosTotal) {
                        query(".badge.ipojos", this.domNode).addClass('failure').addContent(this.ipojosActive + '/' + this.ipojosTotal);
                    }
                    else {
                        query(".badge.ipojos", this.domNode).addClass('success').addContent(this.ipojosActive + '/' + this.ipojosTotal);
                    }
                    this.refreshDynamicBadges();
                },
                refreshMemoryBadge: function(data) {
                    var start = data.search(/var __pools__ = /);
                    if (start != -1) {
                        var end = data.substr(start+16).search(/];/);
                        if (end != -1) {
                            var stats = JSON.parse(data.substr(start+16, end+1).replace(/'/g, "\""));
                            this.memoryUsed = stats[stats.length-1].used;
                            query(".badge.memory", this.domNode).addContent(this.memoryUsed);
                        }
                    }
                },
                refreshDynamicBadges: function() {
                    query(".badge.success", this.domNode).style('backgroundColor', '#4b2');
                    query(".badge.failure", this.domNode).style('backgroundColor', '#c54');
                }
            });
        });