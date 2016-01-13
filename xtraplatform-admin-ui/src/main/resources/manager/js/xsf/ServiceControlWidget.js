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
    "dijit/_WidgetBase",
    "dijit/_TemplatedMixin",
    "dojo/text!./ServiceControlWidget/template.html",
    "dojo/on",
    "dojo/mouse",
    "dojo/dom-class",
    "dojo/_base/fx",
    "dojo/fx",
    "dojo/store/JsonRest",
    "dojo/when",
    "dojo/_base/array",
    "dijit/Tooltip",
    "xsf/ConfirmationDialogWidget",
    "xsf/NotificationsInfoWidget",
    "dojox/html/ellipsis",
    "dojo/i18n",
    "dojo/string",
    "xsf/ServiceEditSecurityWidget",
    "dojo/dom-style",
    "dojo/dom-attr",
    "xsf/api/SecurityScopeDecider",
    "xsf/api/Scope"
],
        function (declare, lang, WidgetBase, TemplatedMixin, template, on, mouse, domClass, fx, dfx, JsonRest, when, array,
                ToolTip, ConfirmationDialogWidget, NotificationsInfoWidget, ellipsis, i18n, string, ServiceEditSecurityWidget,
                domStyle, domAttr, SSD, scope) {
            return declare([WidgetBase, TemplatedMixin], {
                STATUS_RUNNING: "STARTED",
                STATUS_STOPPED: "STOPPED",
                STATUS_STARTING: "STARTING",
                STATUS_STOPPING: "STOPPING",
                ICON_PLAY: "fa-play",
                ICON_STOP: "fa-stop",
                ICON_LOCK: "icon-lock",
                ICON_UNLOCK: "icon-unlock",
                ICON_USER_LOCK: "icon-user",
                statusIconClass: "",
                securityIconClass: "",
                statusIconHover: null,
                statusIconUnhover: null,
                statusIconClick: null,
                statusIconAnim: null,
                id: "",
                browserlang: "",
                type: "",
                name: "",
                status: "",
                browseUrl: "",
                mapAppsUrl: "",
                notifications: null,
                notificationsLocal: null,
                notificationText: "",
                hasNotifications: false,
                templateString: template,
                baseClass: "serviceControlWidget",
                adminStore: null,
                pageWidget: null,
                service: null,
                modules: [],
                editSecurityDialog: null,
                securitySettings: null,
                viewerurl: "",
                constructor: function (service, other) {
                    lang.mixin(this, service);
                    lang.mixin(this, other);

                    this.messages = i18n.getLocalization("xsf", "Module", navigator.userLanguage || navigator.language);

                    var brlang = navigator.userLanguage || navigator.language;
                    if (brlang === "de" || brlang === "de_DE" || brlang === "de-DE") {
                        this.browserlang = "de";
                    } else {
                        this.browserlang = "en";
                    }
                                        
                    this.service = service;

                    this.adminStore = this.adminStore.getSubStore("services/");

                    if (this.isStopped()) {
                        this.statusIconClass = this.ICON_STOP;
                    }
                    else if (this.isRunning()) {
                        this.statusIconClass = this.ICON_PLAY;
                    }

                    this.securityIconClass = this.ICON_LOCK;

                    var lng = navigator.userLanguage || navigator.language;
                    if (lng.substring(0, 2) === "de") {
                        this.notificationsLocal = this.notifications.de;
                    } else {
                        this.notificationsLocal = this.notifications.en;
                    }

                    array.forEach(this.notificationsLocal, function (entry, i) {
                        if (i > 0)
                            this.notificationText += '&#13';
                        this.notificationText += entry.message;
                        this.hasNotifications = true;
                    }, this);
                    
                    array.forEach(this.modules, function (entry, i) {
                            array.forEach(entry.getServiceViews(), function (item, k) {                        
                                this.viewerurl = string.substitute(item.link, { id: this.id, browserlang: this.browserlang });      
                        }, this);
                    }, this);
                },
                postCreate: function ()
                {
                    var ssd = new SSD({store: this.adminStore});
                    if (!ssd.allowed(scope.PUBLISHER)) {
                        this.hideNode(this.securityControl);
                        this.hideNode(this.editControl);
                        this.hideNode(this.removeControl);
                        this.hideNode(this.statusControlParent);
                    }
                    
                    if( this.viewerurl === "") {
                        this.hideNode(this.viewControl);
                    }
                    
                    // Remove the slash at the end of the URL
                    if( this.browseControl.href.endsWith("/")) {
                        this.browseControl.href = this.browseControl.href.substring(0,this.browseControl.href.length-1);
                    }
                                        
                    this.statusIconAnimLight = fx.animateProperty({
                        node: this.statusControl,
                        properties: {
                            color: {
                                start: "#404040",
                                end: "#D6D6D6"
                            }
                        },
                        duration: 500
                    });

                    this.statusIconAnimDark = fx.animateProperty({
                        node: this.statusControl,
                        properties: {
                            color: {
                                start: "#D6D6D6",
                                end: "#404040"
                            }
                        },
                        duration: 500
                    });

                    if (this.hasNotifications) {
                        domClass.remove(this.notificationControl, "dijitHidden");
                    }

                    this._initStatus();

                    on(this.editControl, "click", lang.hitch(this, this._loadServiceConfig));
                    on(this.removeControl, "click", lang.hitch(this, this._deleteService));
                    on(this.securityControl, "click", lang.hitch(this, this._loadSecuritySettings));
                    on(this.notificationControl, "click", lang.hitch(this, this._loadNotificationInfo));

                    this.initSecuritySettings();
                },
                _loadNotificationInfo: function () {
                    var info = new NotificationsInfoWidget({notifications: this.notificationsLocal, adminStore: this.adminStore.getSubStore(this.id)});
                    info.show();
                },
                initSecuritySettings: function () {
                    when(this.adminStore.get("../permissions/services-" + this.id), lang.hitch(this, this._initSecuritySettings));
                },
                _initSecuritySettings: function (securitySettings) {

                    if (securitySettings.obligation === "PRIVATE" || securitySettings.obligation === "PRIVATE_WITH_GROUPS") {
                        domClass.replace(this.securityStatusIcon, this.ICON_LOCK, this.ICON_UNLOCK);

                        var url = domAttr.get(this.browseControl, 'href');
                        var tokPos = url.indexOf('?token=');
                        if (tokPos > -1) {
                            domAttr.set(this.browseControl, 'href', url.substr(0, tokPos) + '?token=' + this.adminStore.getToken());
                        }
                        else {
                            domAttr.set(this.browseControl, 'href', url + '?token=' + this.adminStore.getToken());
                        }
                    }
                    else {
                        domClass.replace(this.securityStatusIcon, this.ICON_UNLOCK, this.ICON_LOCK);
                    }
                },
                _deleteService: function () {
                    var confirm = new ConfirmationDialogWidget({
                        title: dojo.string.substitute(this.messages.deleteServiceId, {id: this.id}),
                        content: dojo.string.substitute(this.messages.deleteServiceMessage, {id: this.id}),
                        action: lang.hitch(this, function () {
                            when(this.adminStore.remove(this.id), lang.hitch(this, this._serviceDeleted));
                        })
                    });
                    confirm.onHide = lang.hitch(confirm, function () {
                        this.destroyRecursive();
                    });
                    confirm.show();

                },
                _serviceDeleted: function () {
                    lang.hitch(this.pageWidget, this.pageWidget.success, dojo.string.substitute(this.messages.deleteServiceSuccess, {id: this.id}), 5000)();
                    lang.hitch(this.pageWidget, this.pageWidget.refreshServices)();
                },
                _loadServiceConfig: function () {
                    when(this.adminStore.get(this.id + "/config"), lang.hitch(this, this._loadEditServiceDialog));
                },
                _loadEditServiceDialog: function (config) {
                    require(["xsf/" + this.type + "/ServiceEditDialogWidget"], lang.hitch(this, this._showEditServiceDialog, this.type, config));
                },
                _showEditServiceDialog: function (type, config, SubServiceEditWidget) {
                    this.editServiceDialog = new SubServiceEditWidget({
                        adminStore: this.adminStore,
                        manualProvider: this.manualProvider,
                        pageWidget: this.pageWidget,
                        service: this.service,
                        config: config,
                        modules: this.modules
                    });
                    this.editServiceDialog.onHide = lang.hitch(this, this._destroyEditServiceDialog);
                    this.editServiceDialog.show();
                },
                _destroyEditServiceDialog: function () {

                    this.name = this.editServiceDialog.config.name;
                    this.serviceName.innerHTML = "<b>[" + this.id + "]&nbsp;" + this.name + "</b>";

                    this.editServiceDialog.destroyRecursive();
                },
                _loadSecuritySettings: function () {
                    when(this.adminStore.get("../permissions/services-" + this.id), lang.hitch(this, this._loadEditSecurityDialog));
                },
                _loadEditSecurityDialog: function (settings) {
                    this.securitySettings = settings;
                    when(this.adminStore.get("../groups/"), lang.hitch(this, this._showEditSecurityDialog));
                },
                _showEditSecurityDialog: function (groupslist) {
                    this.editSecurityDialog = new ServiceEditSecurityWidget({
                        adminStore: this.adminStore,
                        manualProvider: this.manualProvider,
                        pageWidget: this.pageWidget,
                        service: this.service,
                        groupslist: groupslist,
                        title: string.substitute(this.messages.editSecuritySettings, {name: this.service.id}),
                        settings: this.securitySettings
                    });
                    this.editSecurityDialog.onHide = lang.hitch(this, this._destroyEditSecurityDialog);
                    this.editSecurityDialog.show();
                },
                _destroyEditSecurityDialog: function () {

                    //console.log("_destroyEditSecurityDialog");
                    //this.name = this.editSecurityDialog.config.name;                   
                    //this.serviceName.innerHTML = "<b>["+this.id+"]&nbsp;"+this.name+"</b>";
                    this.initSecuritySettings();
                    this.editSecurityDialog.destroyRecursive();
                },
                _initStatus: function () {
                    if (this.statusIconHover)
                        this.statusIconHover.remove();
                    if (this.statusIconUnhover)
                        this.statusIconUnhover.remove();
                    if (this.statusIconClick)
                        this.statusIconClick.remove();
                    if (this.statusIconAnim)
                        this.statusIconAnim.stop(true);
                    if (this.statusIconTimeout)
                        clearTimeout(this.statusIconTimeout);

                    if (this.isStarting() || this.isStopping()) {

                        if (this.isStarting()) {
                            this.statusControl.title = dojo.string.substitute(this.messages.startingService, {id: this.id});
                        }
                        else if (this.isStopping()) {
                            this.statusControl.title = dojo.string.substitute(this.messages.stoppingService, {id: this.id});
                        }

                        //this._blink();
                    }
                    else {
                        this.statusIconHover = on(this.statusControl, mouse.enter, lang.hitch(this, this._hover));
                        this.statusIconUnhover = on(this.statusControl, mouse.leave, lang.hitch(this, this._unhover));
                        this.statusIconClick = on(this.statusControl, "click", lang.hitch(this, this._toggleStatus));

                        if (this.isStopped()) {
                            this.statusControl.title = dojo.string.substitute(this.messages.startService, {id: this.id});
                        }
                        else if (this.isRunning()) {
                            this.statusControl.title = dojo.string.substitute(this.messages.stopService, {id: this.id});
                        }
                    }

                },
                _blink: function () {

                    this.statusIconTimeout = setTimeout(lang.hitch(this, this._blink), 2000);

                    if (this.isStarting())
                        this.statusIconAnim = dfx.chain([this.statusIconAnimLight, this.statusIconAnimDark]);
                    if (this.isStopping())
                        this.statusIconAnim = dfx.chain([this.statusIconAnimDark, this.statusIconAnimLight]);

                    this.statusIconAnim.play();
                    //}
                },
                _hover: function () {
                    if (this.isStopped()) {
                        domClass.replace(this.statusControlIcon, this.ICON_PLAY, this.ICON_STOP);
                    }
                    else if (this.isRunning()) {
                        domClass.replace(this.statusControlIcon, this.ICON_STOP, this.ICON_PLAY);
                    }
                },
                _unhover: function () {
                    if (this.isStopped()) {
                        domClass.replace(this.statusControlIcon, this.ICON_STOP, this.ICON_PLAY);
                    }
                    else if (this.isRunning()) {
                        domClass.replace(this.statusControlIcon, this.ICON_PLAY, this.ICON_STOP);
                    }
                },
                _toggleStatus: function (evt) {
                    evt.preventDefault();
                    evt.stopPropagation();

                    if (this.isStopped()) {
                        this._startService();
                    }
                    else if (this.isRunning()) {
                        this._stopService();
                    }
                },
                _startService: function () {
                    this.status = this.STATUS_STARTING;
                    domClass.replace(this.statusControlParent, this.STATUS_STARTING, this.STATUS_STOPPED);
                    this._initStatus();

                    when(this.adminStore.put({
                        targetStatus: "STARTED"
                    }, {
                        id: this.id,
                        incremental: true
                    }), lang.hitch(this, this._onSuccess), lang.hitch(this, this._onFailure));
                },
                _stopService: function () {
                    this.status = this.STATUS_STOPPING;
                    domClass.replace(this.statusControlParent, this.STATUS_STOPPING, this.STATUS_RUNNING);
                    this._initStatus();

                    when(this.adminStore.put({
                        targetStatus: "STOPPED"
                    }, {
                        id: this.id,
                        incremental: true
                    }), lang.hitch(this, this._onSuccess), lang.hitch(this, this._onFailure));
                },
                isRunning: function () {
                    return this.status === this.STATUS_RUNNING;
                },
                isStopped: function () {
                    return this.status === this.STATUS_STOPPED;
                },
                isStarting: function () {
                    return this.status === this.STATUS_STARTING;
                },
                isStopping: function () {
                    return this.status === this.STATUS_STOPPING;
                },
                _onSuccess: function () {
                    if (this.isStopping()) {
                        this.status = this.STATUS_STOPPED;
                        domClass.replace(this.statusControlParent, this.STATUS_STOPPED, this.STATUS_STOPPING);
                        this._initStatus();
                        lang.hitch(this.pageWidget, this.pageWidget.success,
                                dojo.string.substitute(this.messages.stopServiceSuccess, {id: this.id}), 5000)();
                    }
                    else if (this.isStarting()) {
                        this.status = this.STATUS_RUNNING;
                        domClass.replace(this.statusControlParent, this.STATUS_RUNNING, this.STATUS_STARTING);
                        this._initStatus();
                        lang.hitch(this.pageWidget, this.pageWidget.success,
                                dojo.string.substitute(this.messages.startServiceSuccess, {id: this.id}), 5000)();
                    }
                },
                _onFailure: function () {
                    if (this.isStopping()) {
                        this.status = this.STATUS_RUNNING;
                        domClass.replace(this.statusControlParent, this.STATUS_RUNNING, this.STATUS_STARTING);
                        this._initStatus();
                        lang.hitch(this.pageWidget, this.pageWidget.error,
                                dojo.string.substitute(this.messages.stopServiceFail, {id: this.id}), 0)();
                    }
                    else if (this.isStarting()) {
                        this.status = this.STATUS_STOPPED;
                        domClass.replace(this.statusControlParent, this.STATUS_STOPPED, this.STATUS_STOPPING);
                        this._initStatus();
                        lang.hitch(this.pageWidget, this.pageWidget.error,
                                dojo.string.substitute(this.messages.startServiceFail, {id: this.id}), 0)();
                    }
                },
                hideNode: function (node) {
                    domStyle.set(node, 'display', 'none');
                },
                showNode: function (node) {
                    domStyle.set(node, 'display', '');
                },
            });
        });
