(self.webpackChunk_N_E=self.webpackChunk_N_E||[]).push([[58],{9476:function(e,s,t){Promise.resolve().then(t.bind(t,767))},767:function(e,s,t){"use strict";t.r(s);var n=t(3827),a=t(2169),l=t(2177),i=t(7907),c=t(4090),r=t(6162),o=t(5815),d=t(594),m=t(7780),u=t(5744),h=t(6054),x=t.n(h);t(1209),t(7613);var f=t(9539),p=t.n(f);function g(){let e=(0,i.useRouter)(),[s,t]=(0,c.useState)([]),[h,f]=(0,c.useState)(null),[g,j]=(0,c.useState)([]),[b,y]=(0,c.useState)({}),[v,w]=(0,c.useState)(!0),[N,k]=(0,c.useState)([]),[S,E]=(0,c.useState)("overview"),[_,L]=(0,c.useState)(!1),C="",H=(0,i.useSearchParams)();null!==H&&(C=H.get("id")),(0,c.useEffect)(()=>{if(g&&h){let e=g.filter(e=>e.name==="entities/".concat(h.type,"/").concat(h.id)).flatMap(e=>e.capabilities?e.capabilities.map(s=>({...s,timestamp:e.timestamp,components:e.components?e.components.filter(e=>e.capabilities.includes(s.name)).map(e=>({...e,capability:s.name,component:!0})):[]})):[]).map(e=>({label:e.name,status:e.state,message:e.message,checked:p()(e.timestamp).format("HH:mm:ss"),subRows:e.components.map(e=>({label:e.name,status:e.state,message:e.message,checked:""}))}));k(e),r.D&&console.log("myCheck:",e)}},[g,h]);let O=async()=>{try{let e=await (0,a.fL)();j(e)}catch(e){console.error("Error loading health checks:",e)}},D=async()=>{try{if(null!==C){let e=await (0,a.ZH)(C);"Method not allowed"===e.message?L(!0):y(e)}}catch(e){console.error("Error loading cfg:",e),L(!0)}},z=async()=>{try{let e=await (0,a.fi)();if(!e)return(0,i.notFound)();t(e);let s=e.find(e=>e.uid===C);f(s),r.D&&(console.log("newEntities",e),console.log("myEntity",s))}catch(e){console.error("Error loading entities:",e)}};return((0,c.useEffect)(()=>{(async()=>{await z(),await O(),await D(),w(!1),r.D&&(console.log("entities[id]",s),console.log("entity[id]",h),console.log("cfg",b))})()},[]),v)?(0,n.jsxs)("div",{className:"flex items-center",children:[(0,n.jsx)(m.Z,{color:"#123abc",loading:!0,size:20}),(0,n.jsx)("span",{style:{marginLeft:"10px"},children:"Loading..."})]}):(0,n.jsxs)(n.Fragment,{children:[(0,n.jsx)("div",{className:"flex justify-between items-center",children:(0,n.jsxs)("div",{className:"flex items-center",children:[(0,n.jsx)("a",{onClick:()=>e.back(),className:"font-bold flex items-center cursor-pointer text-blue-500 hover:text-blue-400",children:(0,n.jsx)(l.wyc,{className:"mr-[-1px] h-6 w-6"})}),(0,n.jsx)("h2",{className:"text-2xl font-semibold tracking-tight ml-2",children:h?h.id:"Not Found..."})]})}),(0,n.jsx)("div",{className:"p-8 pt-6",children:(0,n.jsxs)(u.mQ,{value:S,onValueChange:e=>{E(e)},className:"h-full space-y-6",children:[(0,n.jsx)("div",{className:"space-between flex items-center",children:(0,n.jsx)(u.dr,{children:(0,n.jsx)(u.SP,{value:"overview",children:(0,n.jsx)("span",{children:"Health"})})})}),(0,n.jsx)(u.nU,{value:"overview",children:(0,n.jsxs)("div",{children:[(0,n.jsx)("p",{className:"text-sm text-muted-foreground mb-4",children:"Health checks for the capabilities of this entity."}),(0,n.jsx)(d.w,{columns:o.z,data:N})]})}),(0,n.jsx)(u.nU,{value:"cfg",children:(0,n.jsx)("div",{style:{backgroundColor:"#f5f5f5",borderRadius:"8px",padding:"16px",border:"1px solid lightgray"},children:_?"No results.":0===Object.keys(b).length?(0,n.jsxs)("div",{className:"flex items-center",children:[(0,n.jsx)(m.Z,{color:"#123abc",loading:!0,size:20}),(0,n.jsx)("span",{style:{marginLeft:"5px"},children:"Loading..."})]}):Object.entries(b).map(e=>{let[s,t]=e,a=JSON.stringify(t,null,2),l=x().highlight(a,x().languages.json,"json");return(0,n.jsxs)("div",{style:{display:"flex"},children:[(0,n.jsxs)("span",{children:[s,":"]}),(0,n.jsx)("pre",{dangerouslySetInnerHTML:{__html:l},style:{margin:"0 0 0 10px"}})]},s)})})})]})})]})}s.default=()=>(0,n.jsx)(c.Suspense,{fallback:(0,n.jsx)("div",{children:"Loading..."}),children:(0,n.jsx)(g,{})})},6162:function(e,s,t){"use strict";t.d(s,{D:function(){return n}});let n=!0}},function(e){e.O(0,[310,285,639,788,267,549,971,69,744],function(){return e(e.s=9476)}),_N_E=e.O()}]);