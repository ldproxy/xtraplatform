(self.webpackChunk_N_E=self.webpackChunk_N_E||[]).push([[146],{5531:function(e,t,r){Promise.resolve().then(r.bind(r,3813))},3813:function(e,t,r){"use strict";r.r(t),r.d(t,{default:function(){return f}});var n=r(3827),a=r(4747),s=r(5744),i=r(7907),c=r(2169),l=r(4090),o=r(6162),u=r(3702),d=r(8403);function f(){let[e,t]=(0,l.useState)([]),[r,f]=(0,l.useState)("overview"),m=(0,i.useRouter)(),p=(0,i.usePathname)(),x=e.map(d.RL).filter((e,t,r)=>r.indexOf(e)===t),g=e.reduce((e,t)=>{let r=(0,d.RL)(t);return e[r]||(e[r]=0),e[r]++,e},{}),h=(0,d.zM)(e,x),v=async()=>{try{let e=await (0,c.fi)(),r=await (0,c.fL)();e.forEach(e=>{let t=r.find(t=>t.name==="entities/".concat(e.type,"/").concat(e.id));e.status=t&&t.state?t.state:"UNKNOWN"}),t(e)}catch(e){console.error("Error loading entities:",e)}};return(0,l.useEffect)(()=>{v(),p&&f(window.location.hash.slice(1)||"overview")},[]),o.D&&(console.log("entityTypeStatusCounts:",h),console.log("entities",e),console.log("Counts:",g.API),console.log("entityTypes",x)),(0,n.jsxs)("div",{className:"flex-1 space-y-4 p-8 pt-0",children:[(0,n.jsx)("div",{className:"flex items-center justify-between space-y-2",children:(0,n.jsx)("h2",{className:"text-2xl font-semibold tracking-tight",children:"Entities"})}),(0,n.jsxs)(s.mQ,{value:r,onValueChange:e=>{f(e),m.push("".concat(p,"#").concat(e))},className:"h-full space-y-6",children:[(0,n.jsx)("div",{className:"space-between flex items-center",children:(0,n.jsxs)(s.dr,{children:[(0,n.jsx)(s.SP,{value:"overview",children:(0,n.jsx)("span",{children:"Overview"})}),x.map(e=>(0,n.jsx)(s.SP,{value:e,children:(0,n.jsx)("span",{children:(0,d.Xd)(e)})},e))]})}),(0,n.jsx)(s.nU,{value:"overview",children:(0,n.jsx)("div",{className:"grid gap-4 md:grid-cols-2 lg:grid-cols-4",children:x.map(e=>(0,n.jsx)(a.Z,{main:(0,d.Xd)(e),footer:(0,d.Ph)(h[e]),total:g[e],onClick:()=>f(e),route:"".concat(p,"#").concat(e),Icon:(0,u.q)("Id")},e))})}),x.map(t=>(0,n.jsx)(s.nU,{value:t,children:(0,n.jsx)("div",{className:"grid gap-4 md:grid-cols-2 lg:grid-cols-4",children:e.filter(e=>(0,d.RL)(e)===t).map(e=>(0,n.jsx)(a.Z,{header:e.status,main:e.id,footer:e.subType.toUpperCase(),route:"/entities/details?id=".concat(e.uid)},e.uid))})},t))]})]})}},4747:function(e,t,r){"use strict";r.d(t,{Z:function(){return o}});var n=r(3827),a=r(8792),s=r(9084),i=r(2177),c=r(5479);let l=e=>{let{footer:t}=e,r=t.includes("/")?t.split("/")[1]:t.includes("OGC_API")?t:null;return console.log("footer: ",t),console.log("type1: ",r),(0,n.jsx)(n.Fragment,{children:t?t.split(" ").map((e,t,a)=>"available"!==e||isNaN(Number(a[t-1]))?"limited"!==e||isNaN(Number(a[t-1]))?"unavailable"!==e||isNaN(Number(a[t-1]))?null!==r?(0,n.jsx)(c.C,{variant:"secondary",children:r},r):isNaN(Number(e))||"available"!==a[t+1]&&"unavailable"!==a[t+1]&&"limited"!==a[t+1]?e+" ":null:[(0,n.jsxs)("span",{className:"text-destructive mr-2",style:{display:"flex",alignItems:"center"},children:[(0,n.jsx)(i.xrR,{className:"text-destructive",style:{marginRight:"3px"}},"CheckCircledIcon"),a[t-1]," ",e]},t)]:[(0,n.jsxs)("span",{className:"text-warning mr-2",style:{display:"flex",alignItems:"center"},children:[(0,n.jsx)(i.LPM,{className:"text-warning",style:{marginRight:"3px"}},"CheckCircledIcon"),a[t-1]," ",e]},t)]:[(0,n.jsxs)("span",{className:"text-success mr-2",style:{display:"flex",alignItems:"center",marginRight:"15px"},children:[(0,n.jsx)(i.NhS,{className:"text-success",style:{marginRight:"3px"}},"CheckCircledIcon"),a[t-1]," ",e]},t)]):null})};function o(e){let{header:t,main:r,footer:i,route:c,total:o,Icon:u,...d}=e,f="AVAILABLE"===t||"ACTIVE"===t||"true"===t?"text-success":"LIMITED"===t?"text-warning":"UNAVAILABLE"===t||"false"===t?"text-destructive":"text-blue-700";return(0,n.jsx)(a.default,{href:c||"#",children:(0,n.jsxs)(s.Zb,{className:"shadow-lg hover:bg-gray-100 transition-colors duration-200",...d,children:[(0,n.jsx)(s.Ol,{className:"flex flex-row items-center justify-between space-y-0 pb-0",children:(0,n.jsx)(s.ll,{className:"text-sm font-semibold ".concat(f).concat(t?"":"text-2xl font-bold mb-1"),children:t||(0,n.jsx)("span",{className:"text-2xl font-bold mb-1",children:o})})}),(0,n.jsxs)(s.aY,{children:[(0,n.jsxs)("div",{className:"text-2xl font-bold",style:{display:"flex",flexDirection:"row",marginBottom:"5px",alignItems:"center",justifyContent:"flex-start",gap:"10px"},children:[u?(0,n.jsx)(u,{className:"h-6 w-6 text-muted-foreground"}):null,(0,n.jsx)("span",{style:{textOverflow:"ellipsis",overflow:"hidden",whiteSpace:"nowrap"},title:"string"==typeof r?r:"",children:r})]}),(0,n.jsx)("div",{style:{display:"flex",flexWrap:"wrap"},children:(0,n.jsx)("p",{className:"text-xs text-muted-foreground",style:{display:"flex",flexDirection:"row"},children:i?(0,n.jsx)(l,{footer:i}):null})})]})]})})}},5479:function(e,t,r){"use strict";r.d(t,{C:function(){return c}});var n=r(3827);r(4090);var a=r(7742),s=r(2169);let i=(0,a.j)("inline-flex items-center rounded-full border px-2.5 py-0.5 text-xs font-semibold transition-colors focus:outline-none focus:ring-2 focus:ring-ring focus:ring-offset-2",{variants:{variant:{default:"border-transparent bg-primary text-primary-foreground hover:bg-primary/80",secondary:"border-transparent bg-secondary text-secondary-foreground hover:bg-secondary/80",destructive:"border-transparent bg-destructive text-destructive-foreground hover:bg-destructive/80",success:"border-transparent bg-success text-success-foreground hover:bg-success/80",warning:"border-transparent bg-warning text-warning-foreground hover:bg-warning/80",outline:"text-foreground"}},defaultVariants:{variant:"default"}});function c(e){let{className:t,variant:r,...a}=e;return(0,n.jsx)("div",{className:(0,s.cn)(i({variant:r}),t),...a})}},9084:function(e,t,r){"use strict";r.d(t,{Ol:function(){return c},Zb:function(){return i},aY:function(){return o},ll:function(){return l}});var n=r(3827),a=r(4090),s=r(2169);let i=a.forwardRef((e,t)=>{let{className:r,...a}=e;return(0,n.jsx)("div",{ref:t,className:(0,s.cn)("rounded-lg border bg-card text-card-foreground shadow-sm",r),...a})});i.displayName="Card";let c=a.forwardRef((e,t)=>{let{className:r,...a}=e;return(0,n.jsx)("div",{ref:t,className:(0,s.cn)("flex flex-col space-y-1.5 p-6",r),...a})});c.displayName="CardHeader";let l=a.forwardRef((e,t)=>{let{className:r,...a}=e;return(0,n.jsx)("h3",{ref:t,className:(0,s.cn)("text-2xl font-semibold leading-none tracking-tight",r),...a})});l.displayName="CardTitle",a.forwardRef((e,t)=>{let{className:r,...a}=e;return(0,n.jsx)("p",{ref:t,className:(0,s.cn)("text-sm text-muted-foreground",r),...a})}).displayName="CardDescription";let o=a.forwardRef((e,t)=>{let{className:r,...a}=e;return(0,n.jsx)("div",{ref:t,className:(0,s.cn)("p-6 pt-0",r),...a})});o.displayName="CardContent",a.forwardRef((e,t)=>{let{className:r,...a}=e;return(0,n.jsx)("div",{ref:t,className:(0,s.cn)("flex items-center p-6 pt-0",r),...a})}).displayName="CardFooter"},5744:function(e,t,r){"use strict";r.d(t,{SP:function(){return o},dr:function(){return l},mQ:function(){return c},nU:function(){return u}});var n=r(3827),a=r(4090),s=r(1639),i=r(2169);let c=s.fC,l=a.forwardRef((e,t)=>{let{className:r,...a}=e;return(0,n.jsx)(s.aV,{ref:t,className:(0,i.cn)("inline-flex h-10 items-center justify-center rounded-md bg-muted p-1 text-muted-foreground",r),...a})});l.displayName=s.aV.displayName;let o=a.forwardRef((e,t)=>{let{className:r,...a}=e;return(0,n.jsx)(s.xz,{ref:t,className:(0,i.cn)("inline-flex items-center justify-center whitespace-nowrap rounded-sm px-3 py-1.5 text-sm font-medium ring-offset-background transition-all focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:pointer-events-none disabled:opacity-50 data-[state=active]:bg-background data-[state=active]:text-foreground data-[state=active]:shadow-sm",r),...a})});o.displayName=s.xz.displayName;let u=a.forwardRef((e,t)=>{let{className:r,...a}=e;return(0,n.jsx)(s.VY,{ref:t,className:(0,i.cn)("mt-2 ring-offset-background focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2",r),...a})});u.displayName=s.VY.displayName},6162:function(e,t,r){"use strict";r.d(t,{D:function(){return n}});let n=!0},8403:function(e,t,r){"use strict";r.d(t,{Ph:function(){return c},RL:function(){return n},Xd:function(){return a},lY:function(){return s},zM:function(){return i}});let n=e=>"services"===e.type?"API":e.subType.split(/[/]/)[0],a=e=>e[0].toUpperCase()+e.substring(1)+"s",s=e=>e.reduce((e,t)=>(n(t),e||(e={available:0,limited:0,unavailable:0}),"AVAILABLE"===t.status?e.available++:"LIMITED"===t.status?e.limited++:"UNAVAILABLE"===t.status&&e.unavailable++,e),{available:0,limited:0,unavailable:0}),i=(e,t)=>t.reduce((t,r)=>(t[r]=s(e.filter(e=>n(e)===r)),t),{}),c=e=>{if(!e||!e)return"No entities found";let t="";return e.available&&(t+="".concat(e.available," available")),e.limited&&(t.length>0&&(t+=" "),t+="".concat(e.limited," limited")),e.unavailable&&(t.length>0&&(t+=" "),t+="".concat(e.unavailable," unavailable")),t}},3702:function(e,t,r){"use strict";r.d(t,{c:function(){return a},q:function(){return s}});var n=r(2177);let a={Play:"Play",Id:"Id",Code:"Code",ListBullet:"ListBullet",Upload:"Upload",Clock:"Clock",Desktop:"Desktop"},s=e=>{switch(e){case a.Play:return n.o1U;case a.Id:return n.Xwj;case a.Code:return n.dNJ;case a.ListBullet:return n.jVc;case a.Upload:return n.rG2;case a.Clock:return n.T39;case a.Desktop:return n.ugZ;default:return}}},2169:function(e,t,r){"use strict";r.d(t,{AR:function(){return x},C5:function(){return u},KX:function(){return f},Kt:function(){return d},ZH:function(){return m},cn:function(){return s},fL:function(){return o},fi:function(){return l},hs:function(){return p}});var n=r(3167),a=r(1367);function s(){for(var e=arguments.length,t=Array(e),r=0;r<e;r++)t[r]=arguments[r];return(0,a.m6)((0,n.W)(t))}let i="/api",c="/api",l=async()=>{try{let e=await fetch(i+"/entities"),t=await e.json();return Object.keys(t).flatMap(e=>t[e].map(t=>({type:e,uid:"".concat(e,"_").concat(t.id),...t}))).filter(e=>"DISABLED"!==e.status)}catch(e){throw console.error("Error:",e),e}},o=async()=>{try{let e=await fetch(i+"/health"),t=await e.json();return Object.keys(t).map(e=>({name:e,...t[e],capabilities:t[e].capabilities?Object.keys(t[e].capabilities).map(r=>({name:r,...t[e].capabilities[r]})):void 0,components:t[e].components?Object.keys(t[e].components).map(r=>({name:r,...t[e].components[r]})):void 0}))}catch(e){throw console.error("Error:",e),e}},u=async()=>{try{let e=await fetch(i+"/info");return await e.json()}catch(e){throw console.error("Error:",e),e}},d=async()=>{try{let e=await fetch(i+"/metrics"),t=await e.json();return{uptime:t.gauges["jvm.attribute.uptime"].value,memory:t.gauges["jvm.memory.total.used"].value}}catch(e){throw console.error("Error:",e),e}},f=async()=>{try{let e=await fetch(i+"/values"),t=await e.json();return Object.keys(t).flatMap(e=>t[e].map(t=>({type:e,uid:"".concat(e,"_").concat(t.path),...t})))}catch(e){throw console.error("Error:",e),e}},m=async e=>{try{let t=e.replace(/_/g,"/"),r=await fetch("".concat(c,"/cfg/entities/").concat(t));if(!r.ok)throw Error("HTTP error! status: ".concat(r.status));return await r.json()}catch(e){throw console.error("Error:",e),e}},p=async()=>{try{let e=await fetch(c+"/cfg/global/deployment");if(!e.ok)throw Error("HTTP error! status: ".concat(e.status));return await e.json()}catch(e){throw console.error("Error:",e),e}},x=async e=>{try{let t=e.replace(/_/g,"/"),r=await fetch("".concat(c,"/cfg/values/").concat(t));if(!r.ok)throw Error("HTTP error! status: ".concat(r.status));return await r.json()}catch(e){throw console.error("Error:",e),e}}}},function(e){e.O(0,[310,285,792,639,971,69,744],function(){return e(e.s=5531)}),_N_E=e.O()}]);