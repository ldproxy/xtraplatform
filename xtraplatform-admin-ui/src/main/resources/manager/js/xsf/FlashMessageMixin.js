define([
    "dojo/_base/declare",
    "dojo/_base/lang",
    "dojo/dom",
    "dojo/dom-construct",
    "dojo/_base/fx",
    "dojo/fx/easing",
    "dojo/on"
],
        function(declare, lang, dom, domConstruct, fx, easing, on) {
            return declare(null, {
                flashMessageNode: null,
                messages: [],
                _message: function(text, classes, duration, closebutton) {
                    var message = domConstruct.create("div", {
                        "class": classes,
                        style: {
                            opacity: 0,
                            position: "relative"
                        }
                    }, this.flashMessageNode, "first");

                    var message0 = domConstruct.create("span", {
                        innerHTML: text,
                    }, message);

                    var destroy = lang.hitch(this, this._destroyMessage, message);
                    if (duration) {
                        setTimeout(destroy, duration);
                    }

                    if (closebutton) {
                        var close = domConstruct.create("span", {
                            "class": "icon-remove-circle",
                            style: {
                                position: "absolute",
                                top: "5px",
                                right: "5px",
                                cursor: "pointer"
                            }
                        }, message);

                        on(close, "click", destroy);
                    }

                    fx.fadeIn({
                        node: message,
                        easing: easing.expoOut,
                        duration: 1000
                    }).play();

                    /*var msg = {};
                    msg.text = text;
                    msg.classes = classes;
                    msg.duration = duration;
                    msg.closebutton = closebutton;
                    this.messages.push( msg);*/

                    return destroy;
                },
                _destroyMessage: function(message) {
                    fx.fadeOut({
                        node: message,
                        easing: easing.expoOut,
                        onEnd: lang.hitch(this, function(m) {
                            domConstruct.destroy(m);
                        }, message)
                    }).play();
                },
                progress: function(text, duration) {
                    return this.info("<span class='dijitIcon dijitInline fa fa-spinner fa-spin'></span> " + text, duration);
                },
                info: function(text, duration) {
                    return this._message(text, "alert alert-info", duration);
                },
                error: function(text, duration) {
                    return this._message(text, "alert alert-error", duration, true);
                },
                warn: function(text, duration) {
                    return this._message(text, "alert", duration);
                },
                success: function(text, duration) {
                    return this._message(text, "alert alert-success", duration);
                },
                _initFlashMessages: function() {
                    //console.log("_initFlashMessages ##### ##### #####");
                    
                    //console.log(this.messages);
                    
                }

            });
        });
