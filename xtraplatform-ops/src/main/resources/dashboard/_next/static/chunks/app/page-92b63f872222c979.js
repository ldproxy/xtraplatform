(self.webpackChunk_N_E=self.webpackChunk_N_E||[]).push([[931],{3318:function(n,t,e){Promise.resolve().then(e.bind(e,8836))},7907:function(n,t,e){"use strict";var r=e(5313);e.o(r,"notFound")&&e.d(t,{notFound:function(){return r.notFound}}),e.o(r,"usePathname")&&e.d(t,{usePathname:function(){return r.usePathname}}),e.o(r,"useRouter")&&e.d(t,{useRouter:function(){return r.useRouter}}),e.o(r,"useSearchParams")&&e.d(t,{useSearchParams:function(){return r.useSearchParams}})},2215:function(n){"use strict";var t,e,r,u=n.exports={};function o(){throw Error("setTimeout has not been defined")}function i(){throw Error("clearTimeout has not been defined")}function c(n){if(t===setTimeout)return setTimeout(n,0);if((t===o||!t)&&setTimeout)return t=setTimeout,setTimeout(n,0);try{return t(n,0)}catch(e){try{return t.call(null,n,0)}catch(e){return t.call(this,n,0)}}}!function(){try{t="function"==typeof setTimeout?setTimeout:o}catch(n){t=o}try{e="function"==typeof clearTimeout?clearTimeout:i}catch(n){e=i}}();var s=[],f=!1,a=-1;function l(){f&&r&&(f=!1,r.length?s=r.concat(s):a=-1,s.length&&h())}function h(){if(!f){var n=c(l);f=!0;for(var t=s.length;t;){for(r=s,s=[];++a<t;)r&&r[a].run();a=-1,t=s.length}r=null,f=!1,function(n){if(e===clearTimeout)return clearTimeout(n);if((e===i||!e)&&clearTimeout)return e=clearTimeout,clearTimeout(n);try{e(n)}catch(t){try{return e.call(null,n)}catch(t){return e.call(this,n)}}}(n)}}function m(n,t){this.fun=n,this.array=t}function d(){}u.nextTick=function(n){var t=Array(arguments.length-1);if(arguments.length>1)for(var e=1;e<arguments.length;e++)t[e-1]=arguments[e];s.push(new m(n,t)),1!==s.length||f||c(h)},m.prototype.run=function(){this.fun.apply(null,this.array)},u.title="browser",u.browser=!0,u.env={},u.argv=[],u.version="",u.versions={},u.on=d,u.addListener=d,u.once=d,u.off=d,u.removeListener=d,u.removeAllListeners=d,u.emit=d,u.prependListener=d,u.prependOnceListener=d,u.listeners=function(n){return[]},u.binding=function(n){throw Error("process.binding is not supported")},u.cwd=function(){return"/"},u.chdir=function(n){throw Error("process.chdir is not supported")},u.umask=function(){return 0}},8836:function(n,t,e){"use strict";e.r(t),e.d(t,{default:function(){return c}});var r=e(3827),u=e(4090),o=e(7907),i=e(2172);function c(){let n=(0,o.useRouter)();return(0,u.useEffect)(()=>{i.hB?n.push("/home"):n.push("/deployment")},[n]),(0,r.jsx)("div",{className:"flex-1 space-y-4 p-8 pt-0",children:(0,r.jsx)("div",{className:"flex items-center justify-between space-y-2"})})}},4691:function(n,t,e){"use strict";e.d(t,{CH:function(){return o},Dh:function(){return r},QM:function(){return c},ke:function(){return u},ov:function(){return i}});let r=!1,u=!1,o=!1,i=!1,c=!1},2172:function(n,t,e){"use strict";e.d(t,{CF:function(){return a},TQ:function(){return o},XK:function(){return i},_d:function(){return f},f2:function(){return c},hB:function(){return s}});var r=e(4691),u=e(2215);let o=!1,i=!0,c="saas"===u.env.NEXT_PUBLIC_MULTIPLE_DEPLOYMENTS,s=c||"multi"===u.env.NEXT_PUBLIC_MULTIPLE_DEPLOYMENTS,f=!s&&!c;r.ov&&(console.log("IS_DEV",o),console.log("IS_MODE_SAAS",c),console.log("IS_MODE_MULTI",s),console.log("IS_MODE_SINGLE",f)),o&&u.env.DEPLOYMENTS;let a=o&&u.env.NEXT_PUBLIC_USE_DEV_DATA}},function(n){n.O(0,[971,69,744],function(){return n(n.s=3318)}),_N_E=n.O()}]);