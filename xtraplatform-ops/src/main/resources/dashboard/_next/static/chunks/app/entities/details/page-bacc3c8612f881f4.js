(self.webpackChunk_N_E=self.webpackChunk_N_E||[]).push([[58],{9476:function(e,t,r){Promise.resolve().then(r.bind(r,767))},767:function(e,t,r){"use strict";r.r(t);var n=r(3827),a=r(2169),s=r(2177),i=r(7907),o=r(4090),c=r(6162),l=r(5815),u=r(594),d=r(7780),f=r(4922),m=r(5744),p=r(6054),h=r.n(p);r(1209),r(7613);var g=r(9539),x=r.n(g);function y(){let e=(0,i.useRouter)(),[t,r]=(0,o.useState)([]),[p,g]=(0,o.useState)(null),[y,b]=(0,o.useState)([]),[j,v]=(0,o.useState)({}),[w,N]=(0,o.useState)(!0),[k,E]=(0,o.useState)([]),[S,_]=(0,o.useState)("overview"),[A,C]=(0,o.useState)(!1),[L,R]=(0,o.useState)(!1),[O,H]=(0,o.useState)([]),P="",T=(0,i.useSearchParams)();null!==T&&(P=T.get("id")),(0,o.useEffect)(()=>{P&&R(P.endsWith("tiles"))},[P]),(0,o.useEffect)(()=>{if(y&&p){let e=y.filter(e=>e.name==="entities/".concat(p.type,"/").concat(p.id)).flatMap(e=>e.capabilities?e.capabilities.map(t=>({...t,timestamp:e.timestamp,components:e.components?e.components.filter(e=>e.capabilities.includes(t.name)).map(e=>({...e,capability:t.name,component:!0})):[]})):[]).map(e=>({label:e.name,status:e.state,message:e.message,checked:x()(e.timestamp).format("HH:mm:ss"),subRows:e.components.map(e=>({label:e.name,status:e.state,message:e.message,checked:""}))}));E(e),c.Dh&&console.log("myCheck:",e)}},[y,p]);let D=async()=>{try{let e=await (0,a.fL)();b(e)}catch(e){console.error("Error loading health checks:",e)}},M=async()=>{try{let e=await (0,a.Ly)();H(e)}catch(e){console.error("Error loading jobs:",e)}},U=async()=>{try{let e=await (0,a.fi)();if(!e)return(0,i.notFound)();r(e);let t=e.find(e=>e.uid===P);g(t),c.Dh&&(console.log("newEntities",e),console.log("myEntity",t))}catch(e){console.error("Error loading entities:",e)}};(0,o.useEffect)(()=>{let e=async()=>{await U(),await D(),await M(),N(!1),c.Dh&&(console.log("entities[id]",t),console.log("entity[id]",p),console.log("cfg",j))},r=setInterval(()=>{e()},c.gi);return()=>clearInterval(r)},[L]);let V=[];return(O.length>0&&(V=(0,a.b5)(O)),w)?(0,n.jsxs)("div",{className:"flex items-center",children:[(0,n.jsx)(d.Z,{color:"#123abc",loading:!0,size:20}),(0,n.jsx)("span",{style:{marginLeft:"10px"},children:"Loading..."})]}):(0,n.jsxs)(n.Fragment,{children:[(0,n.jsx)("div",{className:"flex justify-between items-center",children:(0,n.jsxs)("div",{className:"flex items-center",children:[(0,n.jsx)("a",{onClick:()=>e.back(),className:"font-bold flex items-center cursor-pointer text-blue-500 hover:text-blue-400",children:(0,n.jsx)(s.wyc,{className:"mr-[-1px] h-6 w-6"})}),(0,n.jsx)("h2",{className:"text-2xl font-semibold tracking-tight ml-2",children:p?p.id:"Not Found..."})]})}),(0,n.jsx)("div",{className:"p-8 pt-6",children:(0,n.jsxs)(m.mQ,{value:S,onValueChange:e=>{_(e)},className:"h-full space-y-6",children:[(0,n.jsx)("div",{className:"space-between flex items-center",children:(0,n.jsxs)(m.dr,{children:[(0,n.jsx)(m.SP,{value:"overview",children:(0,n.jsx)("span",{children:"Health"})}),P&&L&&O&&O.some(e=>e.entity===(null==P?void 0:P.split("_").slice(1).join("_")))&&(0,n.jsx)(m.SP,{value:"jobs",children:(0,n.jsx)("span",{children:"Jobs"})})]})}),(0,n.jsx)(m.nU,{value:"overview",children:(0,n.jsxs)("div",{children:[(0,n.jsx)("p",{className:"text-sm text-muted-foreground mb-4",children:"Health checks for the capabilities of this entity."}),(0,n.jsx)(u.w,{columns:l.z,data:k})]})}),(0,n.jsx)(m.nU,{value:"jobs",children:P&&V.length>0&&V.filter(e=>e.entity===(null==P?void 0:P.split("_").slice(1).join("_"))).length>0?V.filter(e=>e.entity===(null==P?void 0:P.split("_").slice(1).join("_"))).map(e=>(0,n.jsxs)(n.Fragment,{children:[(0,n.jsx)("div",{className:"grid gap-4 md:grid-cols-1 lg:grid-cols-1",style:{marginBottom:"10px"},children:(0,n.jsx)(f.Z,{entity:e.entity,label:e.label,tilesets:e.details.tileSets,percent:e.percent,startedAt:e.startedAt,updatedAt:e.updatedAt,info:"".concat(e.current,"/").concat(e.total),id:e.id},e.id)}),(0,n.jsx)("div",{className:"grid gap-4 md:grid-cols-2 lg:grid-cols-4"})]})):null}),(0,n.jsx)(m.nU,{value:"cfg",children:(0,n.jsx)("div",{style:{backgroundColor:"#f5f5f5",borderRadius:"8px",padding:"16px",border:"1px solid lightgray"},children:A?"No results.":0===Object.keys(j).length?(0,n.jsxs)("div",{className:"flex items-center",children:[(0,n.jsx)(d.Z,{color:"#123abc",loading:!0,size:20}),(0,n.jsx)("span",{style:{marginLeft:"5px"},children:"Loading..."})]}):Object.entries(j).map(e=>{let[t,r]=e,a=JSON.stringify(r,null,2),s=h().highlight(a,h().languages.json,"json");return(0,n.jsxs)("div",{style:{display:"flex"},children:[(0,n.jsxs)("span",{children:[t,":"]}),(0,n.jsx)("pre",{dangerouslySetInnerHTML:{__html:s},style:{margin:"0 0 0 10px"}})]},t)})})})]})})]})}t.default=()=>(0,n.jsx)(o.Suspense,{fallback:(0,n.jsx)("div",{children:"Loading..."}),children:(0,n.jsx)(y,{})})},5479:function(e,t,r){"use strict";r.d(t,{C:function(){return o}});var n=r(3827);r(4090);var a=r(7742),s=r(2169);let i=(0,a.j)("inline-flex items-center rounded-full border px-2.5 py-0.5 text-xs font-semibold transition-colors focus:outline-none focus:ring-2 focus:ring-ring focus:ring-offset-2",{variants:{variant:{default:"border-transparent bg-primary text-primary-foreground hover:bg-primary/80",secondary:"border-transparent bg-secondary text-secondary-foreground hover:bg-secondary/80",destructive:"border-transparent bg-destructive text-destructive-foreground hover:bg-destructive/80",success:"border-transparent bg-success text-success-foreground hover:bg-success/80",warning:"border-transparent bg-warning text-warning-foreground hover:bg-warning/80",outline:"text-foreground"}},defaultVariants:{variant:"default"}});function o(e){let{className:t,variant:r,...a}=e;return(0,n.jsx)("div",{className:(0,s.cn)(i({variant:r}),t),...a})}},9084:function(e,t,r){"use strict";r.d(t,{Ol:function(){return o},Zb:function(){return i},aY:function(){return l},ll:function(){return c}});var n=r(3827),a=r(4090),s=r(2169);let i=a.forwardRef((e,t)=>{let{className:r,...a}=e;return(0,n.jsx)("div",{ref:t,className:(0,s.cn)("rounded-lg border bg-card text-card-foreground shadow-sm",r),...a})});i.displayName="Card";let o=a.forwardRef((e,t)=>{let{className:r,...a}=e;return(0,n.jsx)("div",{ref:t,className:(0,s.cn)("flex flex-col space-y-1.5 p-6",r),...a})});o.displayName="CardHeader";let c=a.forwardRef((e,t)=>{let{className:r,...a}=e;return(0,n.jsx)("h3",{ref:t,className:(0,s.cn)("text-2xl font-semibold leading-none tracking-tight",r),...a})});c.displayName="CardTitle",a.forwardRef((e,t)=>{let{className:r,...a}=e;return(0,n.jsx)("p",{ref:t,className:(0,s.cn)("text-sm text-muted-foreground",r),...a})}).displayName="CardDescription";let l=a.forwardRef((e,t)=>{let{className:r,...a}=e;return(0,n.jsx)("div",{ref:t,className:(0,s.cn)("p-6 pt-0",r),...a})});l.displayName="CardContent",a.forwardRef((e,t)=>{let{className:r,...a}=e;return(0,n.jsx)("div",{ref:t,className:(0,s.cn)("flex items-center p-6 pt-0",r),...a})}).displayName="CardFooter"},5744:function(e,t,r){"use strict";r.d(t,{SP:function(){return l},dr:function(){return c},mQ:function(){return o},nU:function(){return u}});var n=r(3827),a=r(4090),s=r(1639),i=r(2169);let o=s.fC,c=a.forwardRef((e,t)=>{let{className:r,...a}=e;return(0,n.jsx)(s.aV,{ref:t,className:(0,i.cn)("inline-flex h-10 items-center justify-center rounded-md bg-muted p-1 text-muted-foreground",r),...a})});c.displayName=s.aV.displayName;let l=a.forwardRef((e,t)=>{let{className:r,...a}=e;return(0,n.jsx)(s.xz,{ref:t,className:(0,i.cn)("inline-flex items-center justify-center whitespace-nowrap rounded-sm px-3 py-1.5 text-sm font-medium ring-offset-background transition-all focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:pointer-events-none disabled:opacity-50 data-[state=active]:bg-background data-[state=active]:text-foreground data-[state=active]:shadow-sm",r),...a})});l.displayName=s.xz.displayName;let u=a.forwardRef((e,t)=>{let{className:r,...a}=e;return(0,n.jsx)(s.VY,{ref:t,className:(0,i.cn)("mt-2 ring-offset-background focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2",r),...a})});u.displayName=s.VY.displayName},6162:function(e,t,r){"use strict";r.d(t,{Dh:function(){return n},gi:function(){return s},ke:function(){return a}});let n=!0,a=!1,s=2e3},2169:function(e,t,r){"use strict";r.d(t,{AR:function(){return b},C5:function(){return u},KX:function(){return g},Kt:function(){return d},Ly:function(){return f},R_:function(){return h},ZH:function(){return x},b5:function(){return j},cn:function(){return s},fL:function(){return l},fi:function(){return c},hs:function(){return y}});var n=r(3167),a=r(1367);function s(){for(var e=arguments.length,t=Array(e),r=0;r<e;r++)t[r]=arguments[r];return(0,a.m6)((0,n.W)(t))}r(9079).env.MULTIPLE_DEPLOYMENTS;let i="http://localhost:7081/api",o="/api",c=async()=>{try{let e=await fetch(i+"/entities"),t=await e.json();return Object.keys(t).flatMap(e=>t[e].map(t=>({type:e,uid:"".concat(e,"_").concat(t.id),...t}))).filter(e=>"DISABLED"!==e.status)}catch(e){throw console.error("Error:",e),e}},l=async()=>{try{let e=await fetch(i+"/health"),t=await e.json();return Object.keys(t).map(e=>({name:e,...t[e],capabilities:t[e].capabilities?Object.keys(t[e].capabilities).map(r=>({name:r,...t[e].capabilities[r]})):void 0,components:t[e].components?Object.keys(t[e].components).map(r=>({name:r,...t[e].components[r]})):void 0}))}catch(e){throw console.error("Error:",e),e}},u=async()=>{try{let e=await fetch(i+"/info");return await e.json()}catch(e){throw console.error("Error:",e),e}},d=async()=>{try{let e=await fetch(i+"/metrics"),t=await e.json();return{uptime:t.gauges["jvm.attribute.uptime"].value,memory:t.gauges["jvm.memory.total.used"].value}}catch(e){throw console.error("Error:",e),e}},f=async()=>{try{let e=await fetch(i+"/jobs"),t=await e.json();return m(t.sets)}catch(e){throw console.error("Error:",e),e}},m=e=>{let t=[...e];for(let r of e.flatMap(p))t.some(e=>e.id===r.id)||t.push(r);return t},p=e=>e.followUps.length>0?[e,...e.followUps.flatMap(p)]:[e],h=async()=>{try{let e=await fetch("/api/deployments");return await e.json()}catch(e){throw console.error("Error:",e),e}},g=async()=>{try{let e=await fetch(i+"/values"),t=await e.json();return Object.keys(t).flatMap(e=>t[e].map(t=>({type:e,uid:"".concat(e,"_").concat(t.path),...t})))}catch(e){throw console.error("Error:",e),e}},x=async e=>{try{let t=e.replace(/_/g,"/"),r=await fetch("".concat(o,"/cfg/entities/").concat(t));if(!r.ok)throw Error("HTTP error! status: ".concat(r.status));return await r.json()}catch(e){throw console.error("Error:",e),e}},y=async()=>{try{let e=await fetch(o+"/cfg/global/deployment");if(!e.ok)throw Error("HTTP error! status: ".concat(e.status));return await e.json()}catch(e){throw console.error("Error:",e),e}},b=async e=>{try{let t=e.replace(/_/g,"/"),r=await fetch("".concat(o,"/cfg/values/").concat(t));if(!r.ok)throw Error("HTTP error! status: ".concat(r.status));return await r.json()}catch(e){throw console.error("Error:",e),e}},j=e=>0===e.length?[]:e.sort((e,t)=>100===e.percent&&100!==t.percent?1:100===t.percent&&100!==e.percent?-1:-1===e.startedAt&&t.startedAt>-1?1:-1===t.startedAt&&e.startedAt>-1?-1:e.updatedAt<=0&&t.updatedAt>0?1:t.updatedAt<=0&&e.updatedAt>0?-1:t.startedAt-e.startedAt)}},function(e){e.O(0,[310,658,594,788,267,758,913,971,69,744],function(){return e(e.s=9476)}),_N_E=e.O()}]);