(self.webpackChunk_N_E=self.webpackChunk_N_E||[]).push([[951],{86:function(e,t,n){Promise.resolve().then(n.bind(n,7750))},7750:function(e,t,n){"use strict";n.r(t),n.d(t,{default:function(){return L}});var r=n(3827),a=n(4090),s=n(7134),o=n(3702),l=n(8792),i=n(2169),c=n(5600),d=n(7780),u=n(7907),m=n(9881),f=n(8886),h=n(2177),p=n(1270),x=n(2670),g=n(248),y=n(807),b=n(8196),N=n(1014),j=n(1800),w=n(543),v=n(4691);let E=g.z.object({name:g.z.string().min(1,{message:"Name muss min. 1 Zeichen lang sein."}).max(30,{message:"Username darf max. 30 Zeichen lang sein."}).refine(e=>!/[äöüÄÖÜ]/.test(e),{message:"Username darf keine Umlaute enthalten."}),id:g.z.string().refine(e=>/^\d+$/.test(e),{message:"Id muss eine Zahl sein."}),url:g.z.string().refine(e=>/^[a-zA-Z0-9:.]+$/.test(e),{message:"URL darf nur aus Buchstaben, Zahlen, Doppelpunkten und Punkten bestehen."}),cfg:g.z.string({required_error:"Bitte w\xe4hlen sie eine Konfiguration."})}),I=e=>{let{onSubmit:t}=e,[n,s]=(0,a.useState)(null),[o,l]=(0,a.useState)([]);(0,a.useEffect)(()=>{(0,j.W2)().then(e=>{l(e),v.ke&&console.log("configurations",e)})},[]);let i=(0,x.cI)({resolver:(0,p.F)(E),mode:"onChange",defaultValues:{name:"",id:"",url:""}}),c=async e=>{try{let n=await t(e);n&&(s(n),i.reset())}catch(e){console.error("Fehler beim Absenden des Formulars",e)}};return(0,r.jsxs)(m.cZ,{children:[(0,r.jsxs)(m.fK,{children:[(0,r.jsx)("div",{style:{marginBottom:"15px"},children:(0,r.jsx)(m.$N,{children:"Deployment hinzuf\xfcgen"})}),(0,r.jsx)(N.Z,{})]}),(0,r.jsx)(y.l0,{...i,children:(0,r.jsxs)("form",{onSubmit:i.handleSubmit(c),className:"space-y-8",style:{marginBottom:"25px"},children:[(0,r.jsx)(y.Wi,{control:i.control,name:"name",render:e=>{let{field:t}=e;return(0,r.jsxs)(y.xJ,{children:[(0,r.jsx)(y.lX,{children:"Name"}),(0,r.jsx)(y.NI,{children:(0,r.jsx)(b.I,{placeholder:"Name",...t})}),(0,r.jsx)(y.zG,{})]})}}),(0,r.jsx)(y.Wi,{control:i.control,name:"id",render:e=>{let{field:t}=e;return(0,r.jsxs)(y.xJ,{children:[(0,r.jsx)(y.lX,{children:"ID"}),(0,r.jsx)(y.NI,{children:(0,r.jsx)(b.I,{placeholder:"ID",...t})}),(0,r.jsx)(y.zG,{})]})}}),(0,r.jsx)(y.Wi,{control:i.control,name:"url",render:e=>{let{field:t}=e;return(0,r.jsxs)(y.xJ,{children:[(0,r.jsx)(y.lX,{children:"Url"}),(0,r.jsx)(y.NI,{children:(0,r.jsx)(b.I,{placeholder:"Url",...t})}),(0,r.jsx)(y.zG,{})]})}}),(0,r.jsx)(y.Wi,{control:i.control,name:"cfg",render:e=>{let{field:t}=e;return(0,r.jsxs)(y.xJ,{children:[(0,r.jsxs)(w.Ph,{onValueChange:t.onChange,defaultValue:t.value,children:[(0,r.jsx)(y.NI,{children:(0,r.jsx)(w.i4,{children:(0,r.jsx)(w.ki,{placeholder:"Konfiguration w\xe4hlen..."})})}),(0,r.jsx)(w.Bw,{children:o.map((e,t)=>(0,r.jsx)(w.Ql,{value:e.name,children:e.name},"".concat(t)))})]}),(0,r.jsx)(y.zG,{})]})}}),(0,r.jsx)(f.z,{style:{fontWeight:"bold"},type:"submit",disabled:!i.formState.isValid,children:"Create"})]})}),n&&!n.success?(0,r.jsx)("div",{style:{color:"red"},children:"Ein Fehler ist aufgetreten."}):n&&n.success?(0,r.jsx)("div",{style:{color:"green"},children:"Deployment wurde erfolgreich hinzugef\xfcgt."}):""]})};var F=n(9275),k=n(1066),A=n(2830),D=n(2172);function L(){let e=(0,A.useReloadInterval)(),[t,n]=(0,a.useState)([]),[p,x]=(0,a.useState)([{name:"",availableUrlsCount:0}]),[g,y]=(0,a.useState)([]),[b,N]=(0,a.useState)(!0),[j,w]=(0,a.useState)(!0),[E,L]=(0,a.useState)(null),[C,R]=(0,a.useState)(null),[S,O]=(0,a.useState)(!1),M=(0,u.useRouter)();(0,a.useEffect)(()=>{D._d&&M.replace("/404")},[M]),(0,a.useEffect)(()=>{if((0,s.R_)().then(e=>{n(e)}),e>0){let t=setInterval(()=>{(0,s.R_)().then(e=>{n(e)})},1e3*e);return()=>clearInterval(t)}},[e]),(0,a.useEffect)(()=>{let e=async()=>{N(!0);try{let[e,n]=await Promise.all([(0,F.Vm)(t),(0,k.u)(t,y)]);e&&Object.keys(e).length>0&&T(e)}catch(e){console.error("Ein Fehler ist beim Laden der Daten aufgetreten:",e)}finally{N(!1),w(!1)}};j&&t.length>0&&e()},[t]),(0,a.useEffect)(()=>{if(!j&&e>0){let n=setInterval(async()=>{let[e,n]=await Promise.all([(0,F.Vm)(t),(0,k.u)(t,y)]);e&&Object.keys(e).length>0&&T(e)},1e3*e);return()=>clearInterval(n)}},[t,j,e]);let T=async e=>{let n=await V(e),r=await (0,i.aO)(e,t),a=(0,i.rl)(e,t);L(n),x(r),R(a)},V=async e=>t.length>0?t.map(t=>{let n=e[t.name],r="";return r=n&&n.length>0?n.every(e=>"AVAILABLE"===e.state)?"HEALTHY":n.every(e=>"OFFLINE"===e.state)?"OFFLINE":n.some(e=>"OFFLINE"===e.state)&&n.some(e=>"AVAILABLE"===e.state)?"LIMITED":"AVAILABLE":"OFFLINE",{name:t.name,healthStatus:r}}):null,U=async e=>{try{await (0,s.f8)({name:e.name,apiUrl:["http://".concat(e.url,"/api")],url:"http://".concat(e.url,"/deployment"),id:e.id,cfg:e.cfg});let t=await (0,s.R_)();return n(t),{success:!0}}catch(e){return console.error("Fehler beim Erstellen des Deployments",e),{success:!1}}},z=new URL(window.location.href).origin,P="".concat(z,"/deployment");return(0,r.jsxs)("div",{className:"flex-1 space-y-4 p-8 pt-0",children:[(0,r.jsxs)("div",{className:"flex items-center justify-between space-y-2",children:[(0,r.jsx)("h2",{className:"text-2xl font-semibold tracking-tight",children:"Deployments"}),b&&(0,r.jsx)("div",{className:"ml-auto mr-10",children:(0,r.jsx)(d.Z,{color:"#123abc",loading:!0,size:20})}),D.f2&&(0,r.jsxs)(m.Vq,{onOpenChange:e=>O(e),children:[(0,r.jsxs)(m.hg,{className:(0,f.d)({variant:"default"}),style:{fontWeight:"bold"},children:[(0,r.jsx)(h.SPS,{className:"mr-2 h-4 w-4"}),"Neu"]}),(0,r.jsx)(I,{onSubmit:U})]})]}),(0,r.jsx)("div",{className:"justify-between space-y-2",children:(0,r.jsx)("div",{className:"grid gap-4 md:grid-cols-1 lg:grid-cols-1 ",style:{marginBottom:"10px"},children:t.map((e,t)=>(()=>{var n,a,s;let i=g&&g.find(t=>t.name===e.name),d=E&&(null===(n=E.find(t=>t.name===e.name))||void 0===n?void 0:n.healthStatus),u=(null===(a=p.find(t=>t.name===e.name))||void 0===a?void 0:a.availableUrlsCount)||0,m=C&&(null===(s=C.find(t=>t.name===e.name))||void 0===s?void 0:s.availableUrlsCount)||0;v.CH&&console.log("deploymentInfo",i,"deploymentHealthStatus",d);let f=(0,r.jsx)(c.Z,{name:e.name?" ".concat(e.name):"",url:i&&Array.isArray(i.info)&&i.info.length>0&&"string"==typeof i.info[0].url?i.info[0].url:"",totalNodes:e.apiUrl.length,availableNodes:u,HealthyNodes:m,healthStatus:d&&"string"==typeof d?d:"",IconFooter1:(0,o.q)("InfoCircled"),IconFooter2:(0,o.q)("CheckCircled"),IconFooter3:(0,o.q)("QuestionMark"),className:"hover:bg-gray-100 transition-colors duration-200"},t);return"OFFLINE"===d?(0,r.jsx)("div",{children:f},t):(0,r.jsx)(l.default,{href:"".concat(P,"?did=").concat(e.id),children:f},t)})())})})]})}},9084:function(e,t,n){"use strict";n.d(t,{Ol:function(){return l},Zb:function(){return o},aY:function(){return c},ll:function(){return i}});var r=n(3827),a=n(4090),s=n(2169);let o=a.forwardRef((e,t)=>{let{className:n,...a}=e;return(0,r.jsx)("div",{ref:t,className:(0,s.cn)("rounded-lg border bg-card text-card-foreground shadow-sm",n),...a})});o.displayName="Card";let l=a.forwardRef((e,t)=>{let{className:n,...a}=e;return(0,r.jsx)("div",{ref:t,className:(0,s.cn)("flex flex-col space-y-1.5 p-6",n),...a})});l.displayName="CardHeader";let i=a.forwardRef((e,t)=>{let{className:n,...a}=e;return(0,r.jsx)("h3",{ref:t,className:(0,s.cn)("text-2xl font-semibold leading-none tracking-tight",n),...a})});i.displayName="CardTitle",a.forwardRef((e,t)=>{let{className:n,...a}=e;return(0,r.jsx)("p",{ref:t,className:(0,s.cn)("text-sm text-muted-foreground",n),...a})}).displayName="CardDescription";let c=a.forwardRef((e,t)=>{let{className:n,...a}=e;return(0,r.jsx)("div",{ref:t,className:(0,s.cn)("p-6 pt-0",n),...a})});c.displayName="CardContent",a.forwardRef((e,t)=>{let{className:n,...a}=e;return(0,r.jsx)("div",{ref:t,className:(0,s.cn)("flex items-center p-6 pt-0",n),...a})}).displayName="CardFooter"},9881:function(e,t,n){"use strict";n.d(t,{$N:function(){return p},Vq:function(){return i},cN:function(){return h},cZ:function(){return m},fK:function(){return f},hg:function(){return c}});var r=n(3827),a=n(4090),s=n(8768),o=n(2235),l=n(2169);let i=s.fC,c=s.xz,d=s.h_;s.x8;let u=a.forwardRef((e,t)=>{let{className:n,...a}=e;return(0,r.jsx)(s.aV,{ref:t,className:(0,l.cn)("fixed inset-0 z-50 bg-black/80  data-[state=open]:animate-in data-[state=closed]:animate-out data-[state=closed]:fade-out-0 data-[state=open]:fade-in-0",n),...a})});u.displayName=s.aV.displayName;let m=a.forwardRef((e,t)=>{let{className:n,children:a,...i}=e;return(0,r.jsxs)(d,{children:[(0,r.jsx)(u,{}),(0,r.jsxs)(s.VY,{ref:t,className:(0,l.cn)("fixed left-[50%] top-[50%] z-50 grid w-full max-w-lg translate-x-[-50%] translate-y-[-50%] gap-4 border bg-background p-6 shadow-lg duration-200 data-[state=open]:animate-in data-[state=closed]:animate-out data-[state=closed]:fade-out-0 data-[state=open]:fade-in-0 data-[state=closed]:zoom-out-95 data-[state=open]:zoom-in-95 data-[state=closed]:slide-out-to-left-1/2 data-[state=closed]:slide-out-to-top-[48%] data-[state=open]:slide-in-from-left-1/2 data-[state=open]:slide-in-from-top-[48%] sm:rounded-lg",n),...i,children:[a,(0,r.jsxs)(s.x8,{className:"absolute right-4 top-4 rounded-sm opacity-70 ring-offset-background transition-opacity hover:opacity-100 focus:outline-none focus:ring-2 focus:ring-ring focus:ring-offset-2 disabled:pointer-events-none data-[state=open]:bg-accent data-[state=open]:text-muted-foreground",children:[(0,r.jsx)(o.Z,{className:"h-4 w-4"}),(0,r.jsx)("span",{className:"sr-only",children:"Close"})]})]})]})});m.displayName=s.VY.displayName;let f=e=>{let{className:t,...n}=e;return(0,r.jsx)("div",{className:(0,l.cn)("flex flex-col space-y-1.5 text-center sm:text-left",t),...n})};f.displayName="DialogHeader";let h=e=>{let{className:t,...n}=e;return(0,r.jsx)("div",{className:(0,l.cn)("flex flex-col-reverse sm:flex-row sm:justify-end sm:space-x-2",t),...n})};h.displayName="DialogFooter";let p=a.forwardRef((e,t)=>{let{className:n,...a}=e;return(0,r.jsx)(s.Dx,{ref:t,className:(0,l.cn)("text-lg font-semibold leading-none tracking-tight",n),...a})});p.displayName=s.Dx.displayName,a.forwardRef((e,t)=>{let{className:n,...a}=e;return(0,r.jsx)(s.dk,{ref:t,className:(0,l.cn)("text-sm text-muted-foreground",n),...a})}).displayName=s.dk.displayName},807:function(e,t,n){"use strict";n.d(t,{l0:function(){return u},NI:function(){return y},Wi:function(){return f},xJ:function(){return x},lX:function(){return g},zG:function(){return b}});var r=n(3827),a=n(4090),s=n(15),o=n(2670),l=n(2169),i=n(6028);let c=(0,n(7742).j)("text-sm font-medium leading-none peer-disabled:cursor-not-allowed peer-disabled:opacity-70"),d=a.forwardRef((e,t)=>{let{className:n,...a}=e;return(0,r.jsx)(i.f,{ref:t,className:(0,l.cn)(c(),n),...a})});d.displayName=i.f.displayName;let u=o.RV,m=a.createContext({}),f=e=>{let{...t}=e;return(0,r.jsx)(m.Provider,{value:{name:t.name},children:(0,r.jsx)(o.Qr,{...t})})},h=()=>{let e=a.useContext(m),t=a.useContext(p),{getFieldState:n,formState:r}=(0,o.Gc)(),s=n(e.name,r);if(!e)throw Error("useFormField should be used within <FormField>");let{id:l}=t;return{id:l,name:e.name,formItemId:"".concat(l,"-form-item"),formDescriptionId:"".concat(l,"-form-item-description"),formMessageId:"".concat(l,"-form-item-message"),...s}},p=a.createContext({}),x=a.forwardRef((e,t)=>{let{className:n,...s}=e,o=a.useId();return(0,r.jsx)(p.Provider,{value:{id:o},children:(0,r.jsx)("div",{ref:t,className:(0,l.cn)("space-y-2",n),...s})})});x.displayName="FormItem";let g=a.forwardRef((e,t)=>{let{className:n,...a}=e,{error:s,formItemId:o}=h();return(0,r.jsx)(d,{ref:t,className:(0,l.cn)(s&&"text-destructive",n),htmlFor:o,...a})});g.displayName="FormLabel";let y=a.forwardRef((e,t)=>{let{...n}=e,{error:a,formItemId:o,formDescriptionId:l,formMessageId:i}=h();return(0,r.jsx)(s.g7,{ref:t,id:o,"aria-describedby":a?"".concat(l," ").concat(i):"".concat(l),"aria-invalid":!!a,...n})});y.displayName="FormControl",a.forwardRef((e,t)=>{let{className:n,...a}=e,{formDescriptionId:s}=h();return(0,r.jsx)("p",{ref:t,id:s,className:(0,l.cn)("text-sm text-muted-foreground",n),...a})}).displayName="FormDescription";let b=a.forwardRef((e,t)=>{let{className:n,children:a,...s}=e,{error:o,formMessageId:i}=h(),c=o?String(null==o?void 0:o.message):a;return c?(0,r.jsx)("p",{ref:t,id:i,className:(0,l.cn)("text-sm font-medium text-destructive",n),...s,children:c}):null});b.displayName="FormMessage"},8196:function(e,t,n){"use strict";n.d(t,{I:function(){return o}});var r=n(3827),a=n(4090),s=n(2169);let o=a.forwardRef((e,t)=>{let{className:n,type:a,...o}=e;return(0,r.jsx)("input",{type:a,className:(0,s.cn)("flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background file:border-0 file:bg-transparent file:text-sm file:font-medium placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50",n),ref:t,...o})});o.displayName="Input"},1800:function(e,t,n){"use strict";n.d(t,{vj:function(){return i},ZH:function(){return c},W2:function(){return u},hs:function(){return d},mv:function(){return o},qP:function(){return l}});var r=n(2172),a=n(2215);let s="/api";a.env.DEPLOYMENTS||r.XK;let o=async e=>{try{let t=await fetch("/api/cfg",{method:"POST",headers:{"Content-Type":"application/json"},body:JSON.stringify(e)});return await t.json()}catch(e){throw console.error("Error:",e),e}},l=async(e,t,n,r)=>{try{let a=await fetch("/api/cfg",{method:"PUT",headers:{"Content-Type":"application/json"},body:JSON.stringify({oldName:e,newName:t,oldUrl:n,newUrl:r})});return await a.json()}catch(e){throw console.error("Error:",e),e}},i=async e=>{try{let t=await fetch("/api/cfg",{method:"DELETE",headers:{"Content-Type":"application/json"},body:JSON.stringify({name:e})});if(!t.ok)throw Error("Error: ".concat(t.statusText));return await t.json()}catch(e){throw console.error("Error:",e),e}},c=async e=>{try{let t=e.replace(/_/g,"/"),n=await fetch("".concat(s,"/cfg/entities/").concat(t));if(!n.ok)throw Error("HTTP error! status: ".concat(n.status));return await n.json()}catch(e){throw console.error("Error:",e),e}},d=async()=>{try{let e=await fetch(s+"/cfg/global/deployment");if(!e.ok)throw Error("HTTP error! status: ".concat(e.status));return await e.json()}catch(e){throw console.error("Error:",e),e}},u=async()=>{try{let e=await fetch(s+"/cfg");if(!e.ok)throw Error("HTTP error! status: ".concat(e.status));return await e.json()}catch(e){throw console.error("Error:",e),e}}},9275:function(e,t,n){"use strict";n.d(t,{E8:function(){return i},N5:function(){return l},Vm:function(){return c}});var r=n(9539),a=n.n(r),s=n(9266);let o=(e,t)=>Object.keys(e).map(n=>({name:n,url:t,...e[n],capabilities:e[n].capabilities?Object.keys(e[n].capabilities).map(t=>({name:t,...e[n].capabilities[t]})):void 0,components:e[n].components?Object.keys(e[n].components).map(t=>({name:t,...e[n].components[t]})):void 0})),l=e=>Array.isArray(e)?e.flatMap(e=>e.offline||null===e.response?[{url:e.url,state:"OFFLINE"}]:o(e.response,e.url)):o(e,"TODO");function i(e){let t={},n={};return e.forEach(e=>{t[e.label]||(t[e.label]=0),t[e.label]++}),e.forEach(e=>{if(t[e.label]>1){n[e.label]||(n[e.label]={...e,subRows:[]});let t=n[e.label];t.subRows.push(e),"UNAVAILABLE"===e.state?t.status="UNAVAILABLE":"LIMITED"===e.state&&"UNAVAILABLE"!==t.status?t.status="LIMITED":"AVAILABLE"===e.state&&"UNAVAILABLE"!==t.status&&"LIMITED"!==t.status&&(t.status="AVAILABLE"),a()(e.checked).isAfter(a()(t.checked))&&(t.checked=e.checked)}else n[e.label]=e}),Object.values(n).forEach(e=>{e.subRows&&(e.subRows=e.subRows.filter(t=>t!==e))}),Object.values(n)}let c=async e=>{try{if(e.length>0){let t={},n=e.map(async e=>{try{let n=await (0,s.r)("/api/health",l,!1,e.apiUrl);t[e.name]=n}catch(n){console.error("Error fetching health checks for",e.name,":",n),t[e.name]=[{state:"OFFLINE",url:e.url}]}});return await Promise.all(n),t}}catch(e){console.error("Error loading health checks:",e)}}},1066:function(e,t,n){"use strict";n.d(t,{u:function(){return s},v:function(){return a}});var r=n(9266);let a=e=>Array.isArray(e)?e.map(e=>({...e.response,apiUrl:e.url})):[{...e,apiUrl:"TODO"}],s=async(e,t)=>{try{if(e.length>0){let n=e.map(async e=>{let t=await (0,r.r)("api/info",a,!1,e.apiUrl);return t&&t.length>0?{name:e.name,info:t}:{name:e.name,info:[]}}),s=await Promise.all(n);t(s)}}catch(e){console.error("Error loading info:",e)}}},6311:function(e,t,n){"use strict";n.d(t,{Z:function(){return l}});let r=e=>Number.isFinite(e)?e:0,a=e=>0===e||0n===e,s=(e,t)=>1===t||1n===t?e:"".concat(e,"s"),o=24n*60n*60n*1000n;function l(e,t){let n="bigint"==typeof e;if(!n&&!Number.isFinite(e))throw TypeError("Expected a finite number or bigint");(t={...t}).colonNotation&&(t.compact=!1,t.formatSubMilliseconds=!1,t.separateMilliseconds=!1,t.verbose=!1),t.compact&&(t.unitCount=1,t.secondsDecimalDigits=0,t.millisecondsDecimalDigits=0);let l=[],i=(e,n,r,o)=>{if(!((0===l.length||!t.colonNotation)&&a(e))||t.colonNotation&&"m"===r){if(o=null!=o?o:String(e),t.colonNotation){let e=o.includes(".")?o.split(".")[0].length:o.length,t=l.length>0?2:1;o="0".repeat(Math.max(0,t-e))+o}else o+=t.verbose?" "+s(n,e):r;l.push(o)}},c=function(e){switch(typeof e){case"number":if(Number.isFinite(e))return{days:Math.trunc(e/864e5),hours:Math.trunc(e/36e5%24),minutes:Math.trunc(e/6e4%60),seconds:Math.trunc(e/1e3%60),milliseconds:Math.trunc(e%1e3),microseconds:Math.trunc(r(1e3*e)%1e3),nanoseconds:Math.trunc(r(1e6*e)%1e3)};break;case"bigint":return{days:e/86400000n,hours:e/3600000n%24n,minutes:e/60000n%60n,seconds:e/1000n%60n,milliseconds:e%1000n,microseconds:0n,nanoseconds:0n}}throw TypeError("Expected a finite number or bigint")}(e),d=BigInt(c.days);if(i(d/365n,"year","y"),i(d%365n,"day","d"),i(Number(c.hours),"hour","h"),i(Number(c.minutes),"minute","m"),t.separateMilliseconds||t.formatSubMilliseconds||!t.colonNotation&&e<1e3){let e=Number(c.seconds),n=Number(c.milliseconds),r=Number(c.microseconds),a=Number(c.nanoseconds);if(i(e,"second","s"),t.formatSubMilliseconds)i(n,"millisecond","ms"),i(r,"microsecond","\xb5s"),i(a,"nanosecond","ns");else{let e=n+r/1e3+a/1e6,s="number"==typeof t.millisecondsDecimalDigits?t.millisecondsDecimalDigits:0,o=s?e.toFixed(s):e>=1?Math.round(e):Math.ceil(e);i(Number.parseFloat(o),"millisecond","ms",o)}}else{var u;let r=(Math.round(Math.floor((n?Number(e%o):e)/1e3%60*10**(u="number"==typeof t.secondsDecimalDigits?t.secondsDecimalDigits:1)+1e-7))/10**u).toFixed(u),a=t.keepDecimalsOnWholeSeconds?r:r.replace(/\.0+$/,"");i(Number.parseFloat(a),"second","s",a)}if(0===l.length)return"0"+(t.verbose?" milliseconds":"ms");let m=t.colonNotation?":":" ";return"number"==typeof t.unitCount&&(l=l.slice(0,Math.max(t.unitCount,1))),l.join(m)}}},function(e){e.O(0,[310,606,547,6,950,800,600,971,69,744],function(){return e(e.s=86)}),_N_E=e.O()}]);