define([
    "dojo/_base/declare",
    "dojo/text!./ServiceEditDialogWidget/template.html",
    "dojo/_base/lang",
    "dojo/when",
    'dijit/form/Form',
    "xsf/FormDialogWidgetMixin",
    "xsf/ChildTemplateMixin",
    "xsf/TabMenuWidget",
    "dojo/dom-construct",
    "xsf/ServiceEditGeneralWidget",
    "dojo/_base/array",
    "dojo/dom-form",
    "dijit/ColorPalette",
    "dojo/_base/json",
    "dojo/dom-style",
    "dojo/i18n",
    "dojo/string"
],
        function(declare, template, lang, when, Form, FormDialogWidgetMixin,
                ChildTemplateMixin, TabMenuWidget, domConstruct,
                ServiceEditGeneralWidget, array, dojoform,
                ColorPalette, json, domStyle, i18n, string) {
            return declare([Form, FormDialogWidgetMixin, ChildTemplateMixin], {
                templateString: template,
                baseClass: "expandableListForm",
                adminStore: null,
                title: null,
                service: null,
                config: null,
                modules: [],
                settingsItems: [],
                messages: null,
                constructor: function() {
                    this.messages = i18n.getLocalization("xsf.api", "Module", navigator.userLanguage || navigator.language);
                },
                postMixInProperties: function()
                {
                    if (this.service)
                        this.title = dojo.string.substitute(this.messages.editServiceId, {id: this.service.id});
                    else
                        this.title = this.messages.editService;
                    
                    this.inherited(arguments);

                    //this.style = "width:760px; height: 90%;";

                    //domStyle.set(this.dialog, "height", "90%");

                    //this.dialog.style = "width:760px; height: 90%;";
                },
                postCreate: function() {
                    this.inherited(arguments);

                    // TODO: 
                    array.forEach(this.modules, function(mod, i) {

                        // Die beiden Schleifen löschen ggf das ElternItem aus dem Array wenn 
                        // ein abgeleitetes Item existiert.
                        array.forEach(mod.getServiceSettingsItems(), function(item, j) {
                            array.forEach(this.settingsItems, function(itemg, k) {
                                if (item.id === itemg.id) {
                                    this.settingsItems.splice(k, 1);
                                }
                            }, this);
                        }, this);
                        this.settingsItems = this.settingsItems.concat(mod.getServiceSettingsItems());
                    }, this);

                    var menuContainer = domConstruct.create("ul", {"class": 'sideBarMenu'}, this.menu);
                    var mainMenu = new TabMenuWidget({
                        tabItems: this.settingsItems,
                        widgetParams: {
                            adminStore: this.adminStore,
                            pageWidget: this.pageWidget,
                            service: this.service,
                            config: this.config
                        },
                        domNode: menuContainer,
                        contentContainer: this.content,
                        initAll: true
                    });
                },
                show: function() {
                    this.inherited(arguments);

                    domStyle.set(this.domNode, "height", "");

                },
                onSuccess: function(params, response) {
                    this.destroyProgressMessage();
                    //this.hide();                                       
                    lang.hitch(this.pageWidget, this.pageWidget.success,
                            dojo.string.substitute(this.messages.saveServiceSuccess, {id: this.service.id}), 5000)();
                    lang.hitch(this.pageWidget, this.pageWidget.refreshServices)();
                },
                onFailure: function(params, response) {
                    this.destroyProgressMessage();
                    //this.hide();
                    lang.hitch(this.pageWidget, this.pageWidget.error,
                            dojo.string.substitute(this.messages.saveServiceFailed, {id: this.service.id}), 0)();
                },
                onSubmit: function(params, response) {
                    this.inherited(arguments);

                    var unflatten = function unflatten(target) {
                        var delimiter = '.';
                        var result = {};
                        if (Object.prototype.toString.call(target) !== '[object Object]' && Object.prototype.toString.call(target) !== '[object Array]') {
                            return target;
                        }
                        Object.keys(target).forEach(function(key) {
                            var split = key.split(delimiter)
                                    , firstNibble
                                    , secondNibble
                                    , recipient = result;

                            function getkey(key) {
                                var parsedKey = parseInt(key);
                                return (isNaN(parsedKey) ? key : parsedKey);
                            }
                            ;

                            firstNibble = getkey(split.shift());
                            secondNibble = getkey(split[0]);

                            while (secondNibble !== undefined) {
                                if (recipient[firstNibble] === undefined) {
                                    recipient[firstNibble] = ((typeof secondNibble === 'number') ? [] : {});
                                }

                                recipient = recipient[firstNibble];
                                if (split.length > 0) {
                                    firstNibble = getkey(split.shift());
                                    secondNibble = getkey(split[0]);
                                }
                            }

                            // handle the colorstring
                            if (firstNibble === "color") {
                                if (target[key] !== null && target[key].length > 0) {
                                    var arr = [];
                                    var spl = target[key].split(',');
                                    for (var i = 0; i < spl.length; i++) {
                                        arr[i] = parseInt(spl[i], 10);
                                    }
                                    target[key] = arr;
                                }
                            }

                            if (Object.prototype.toString.call(target[key]) === '[object Array]') {
                                recipient[firstNibble] = [];
                                for (var i in target[key]) {
                                    recipient[firstNibble][i] = unflatten(target[key][i]);
                                }
                            } else {
                                // unflatten again for 'messy objects'
                                recipient[firstNibble] = unflatten(target[key]);
                            }
                        });
                        return result;
                    }; // function unflatten(target) {

                    var service = {};
                    array.forEach(this.settingsItems, function(item, i) {
                        service = lang.mixin(service, item.widgetInstance.gatherFormValues());
                    }, this);

                    service = unflatten(service);

                    for (var i = 0; i < service.fullLayers.length; i++) {
                        if (!service.fullLayers[i]) {
                            service.fullLayers[i] = this.config.fullLayers[i];
                            
                        } else {
                            if (typeof service.fullLayers[i].customDrawingInfo !== "undefined") {

                                if (service.fullLayers[i].customDrawingInfo.length > 0) {
                                    try {
                                        var obj = json.fromJson(service.fullLayers[i].customDrawingInfo);
                                        service.fullLayers[i].drawingInfo = obj;
                                    } catch (e) {
                                        //console.log("Saving changes for service '" + service.id + "' failed. Reason: Validation of Drawing info for Layer '" + i + "' failed: " + e.message);
                                        this.destroyProgressMessage = lang.hitch(this.pageWidget, this.pageWidget.error,
                                                "Saving changes for service '" + service.id + "' failed. Reason: Validation of Drawing info for Layer '" + i + "' failed: " + e.message, 0)();
                                        lang.hitch(this, this.onFailure, service);
                                        this.hide();
                                        return false;
                                    }
                                }
                            }
                        }
                        
                        service.fullLayers[i].enabled = service.layersEnabled[i];
                    }
                    service.layersEnabled = null; 
                                        
                    this.destroyProgressMessage = lang.hitch(this.pageWidget, this.pageWidget.progress, string.substitute(this.messages.saveServiceProgress, {id: service.id}), 0)();
                    when(this.adminStore.put(service, {incremental: true}), lang.hitch(this, this.onSuccess, service), lang.hitch(this, this.onFailure, service));

                    this.hide();

                    /*this.validate();
                     if (this.isValid()) {
                     var params = this.gatherFormValues();
                     console.log(params);
                     this.hide();
                     this.destroyProgressMessage = lang.hitch(this.pageWidget, this.pageWidget.progress, "Saving changes for service '" + params.id + "'", 0)();
                     when(this.adminStore.put(params, {incremental:true}), lang.hitch(this, this.onSuccess, params), lang.hitch(this, this.onFailure, params));
                     
                     }*/
                    return false;
                }
            });
        });