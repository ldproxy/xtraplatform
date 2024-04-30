(self.webpackChunk_N_E=self.webpackChunk_N_E||[]).push([[548],{1645:function(e,t,l){Promise.resolve().then(l.bind(l,5949))},5949:function(e,t,l){"use strict";l.r(t),l.d(t,{default:function(){return h}});var s=l(3827),r=l(4747),a=l(8886),n=l(2177),i=l(5744),c=l(2169),d=l(4090),o=l(3702),x=l(585),u=l(8792);let f=[{accessorKey:"label",header:e=>{let{column:t}=e;return(0,s.jsxs)(a.z,{variant:"ghost",onClick:()=>t.toggleSorting("asc"===t.getIsSorted()),children:["Label",(0,s.jsx)(x.Z,{className:"ml-2 h-4 w-4"})]})},cell:e=>{let{row:t}=e,l=t.original.type.replace(/-/g,"");return(0,s.jsxs)("div",{style:{marginLeft:"10px",fontWeight:"bold"},children:[(0,s.jsx)(u.default,{href:"values/details?id=".concat(l,"_").concat(t.original.label),children:t.original.label})," "]})}}];var p=l(1900),m=l(7907);function h(){let[e,t]=(0,d.useState)([]),[l,x]=(0,d.useState)("overview"),[u,h]=(0,d.useState)([]),g=(0,m.useRouter)(),N=(0,m.usePathname)(),j=async()=>{try{let e=await (0,c.KX)();t(e);let l=e.map(e=>({label:e.id?e.id:e.path,type:e.type}));h(l)}catch(e){console.error("Error loading values:",e)}};(0,d.useEffect)(()=>{j(),N&&x(window.location.hash.slice(1)||"overview")},[]);let y=e.map(e=>e.type).filter((e,t,l)=>l.indexOf(e)===t),v=e.reduce((e,t)=>{let l=t.type;return e[l]||(e[l]=0),e[l]++,e},{});return(0,s.jsxs)("div",{className:"flex-1 space-y-4 p-8 pt-0",children:[(0,s.jsxs)("div",{className:"flex items-center justify-between space-y-2",children:[(0,s.jsx)("h2",{className:"text-2xl font-semibold tracking-tight",children:"Values"}),(0,s.jsx)("div",{className:"flex items-center space-x-2",children:(0,s.jsxs)(a.z,{onClick:j,className:"font-bold",children:[(0,s.jsx)(n.BGW,{className:"mr-2 h-4 w-4"}),"Reload all"]})})]}),(0,s.jsxs)(i.mQ,{value:l,onValueChange:e=>{x(e),g.push("".concat(N,"#").concat(e))},className:"h-full space-y-6",children:[(0,s.jsx)("div",{className:"space-between flex items-center",children:(0,s.jsxs)(i.dr,{children:[(0,s.jsx)(i.SP,{value:"overview",children:(0,s.jsx)("span",{children:"Overview"})}),y.map(e=>(0,s.jsx)(i.SP,{value:e,children:(0,s.jsx)("span",{children:e})},e))]})}),(0,s.jsx)(i.nU,{value:"overview",children:(0,s.jsx)("div",{className:"grid gap-4 md:grid-cols-2 lg:grid-cols-4",children:e.map(e=>(0,s.jsx)(r.Z,{main:e.type,total:v[e.type],onClick:()=>{x(e.type)},Icon:(0,o.q)("Code"),route:"".concat(N,"#").concat(e.type)},e.id?e.id:e.path?e.path:"No Id"))})}),y.map(e=>(0,s.jsx)(i.nU,{value:e,children:(0,s.jsx)(p.w,{columns:f,data:u.filter(t=>t.type===e)})},e))]})]})}},4747:function(e,t,l){"use strict";l.d(t,{Z:function(){return d}});var s=l(3827),r=l(8792),a=l(9084),n=l(2177),i=l(5479);let c=e=>{let{footer:t}=e,l=t.includes("_")?t.split("_")[1]:t.includes("/")?t.split("/")[1]:null;return console.log("footer: ",t),console.log("type1: ",l),(0,s.jsx)(s.Fragment,{children:t?t.split(" ").map((e,t,r)=>"active"!==e||isNaN(Number(r[t-1]))?"defective"!==e||isNaN(Number(r[t-1]))?"true"===e?[(0,s.jsxs)("span",{className:"text-green-500",style:{display:"flex",alignItems:"center",marginRight:"20px"},children:[(0,s.jsx)(n.NhS,{className:"text-green-500",style:{marginRight:"3px"}},"CheckCircledIcon"),"Store is healthy"]},t)]:"false"===e?[(0,s.jsxs)("span",{className:"text-red-500",style:{display:"flex",alignItems:"center",marginRight:"20px"},children:[(0,s.jsx)(n.NhS,{className:"text-red-500",style:{marginRight:"3px"}},"CheckCircledIcon"),"Store is unhealthy"]},t)]:null!==l?(0,s.jsx)(i.C,{children:l},l):isNaN(Number(e))||"active"!==r[t+1]&&"defective"!==r[t+1]?e+" ":null:[(0,s.jsxs)("span",{className:"text-red-500",style:{display:"flex",alignItems:"center"},children:[(0,s.jsx)(n.xrR,{className:"text-red-500",style:{marginRight:"3px"}},"CheckCircledIcon"),r[t-1]," ",e]},t)]:[(0,s.jsxs)("span",{className:"text-green-500",style:{display:"flex",alignItems:"center",marginRight:"15px"},children:[(0,s.jsx)(n.NhS,{className:"text-green-500",style:{marginRight:"3px"}},"CheckCircledIcon"),r[t-1]," ",e]},t)]):null})};function d(e){let{header:t,main:l,footer:n,route:i,total:d,Icon:o,...x}=e;return(0,s.jsx)(r.default,{href:i||"#",children:(0,s.jsxs)(a.Zb,{className:"shadow-lg hover:bg-gray-100 transition-colors duration-200",...x,children:[(0,s.jsx)(a.Ol,{className:"flex flex-row items-center justify-between space-y-0 pb-0",children:(0,s.jsx)(a.ll,{className:"text-sm font-semibold ".concat("ACTIVE"!==t&&"true"!==t&&"HEALTHY"!==t&&isNaN(Number(d))?"text-red-700":"text-blue-700").concat(t?"":"text-2xl font-bold mb-1"),children:t||(0,s.jsx)("span",{className:"text-2xl font-bold mb-1",children:d})})}),(0,s.jsxs)(a.aY,{children:[(0,s.jsxs)("div",{className:"text-2xl font-bold",style:{display:"flex",flexDirection:"row",marginBottom:"5px",alignItems:"center",justifyContent:"flex-start",gap:"10px"},children:[o?(0,s.jsx)(o,{className:"h-6 w-6 text-muted-foreground"}):null,(0,s.jsx)("span",{style:{textOverflow:"ellipsis",overflow:"hidden",whiteSpace:"nowrap"},title:"string"==typeof l?l:"",children:l})]}),(0,s.jsx)("div",{style:{display:"flex",flexWrap:"wrap"},children:(0,s.jsx)("p",{className:"text-xs text-muted-foreground",style:{display:"flex",flexDirection:"row"},children:n?(0,s.jsx)(c,{footer:n}):null})})]})]})})}},9084:function(e,t,l){"use strict";l.d(t,{Ol:function(){return i},Zb:function(){return n},aY:function(){return d},ll:function(){return c}});var s=l(3827),r=l(4090),a=l(2169);let n=r.forwardRef((e,t)=>{let{className:l,...r}=e;return(0,s.jsx)("div",{ref:t,className:(0,a.cn)("rounded-lg border bg-card text-card-foreground shadow-sm",l),...r})});n.displayName="Card";let i=r.forwardRef((e,t)=>{let{className:l,...r}=e;return(0,s.jsx)("div",{ref:t,className:(0,a.cn)("flex flex-col space-y-1.5 p-6",l),...r})});i.displayName="CardHeader";let c=r.forwardRef((e,t)=>{let{className:l,...r}=e;return(0,s.jsx)("h3",{ref:t,className:(0,a.cn)("text-2xl font-semibold leading-none tracking-tight",l),...r})});c.displayName="CardTitle",r.forwardRef((e,t)=>{let{className:l,...r}=e;return(0,s.jsx)("p",{ref:t,className:(0,a.cn)("text-sm text-muted-foreground",l),...r})}).displayName="CardDescription";let d=r.forwardRef((e,t)=>{let{className:l,...r}=e;return(0,s.jsx)("div",{ref:t,className:(0,a.cn)("p-6 pt-0",l),...r})});d.displayName="CardContent",r.forwardRef((e,t)=>{let{className:l,...r}=e;return(0,s.jsx)("div",{ref:t,className:(0,a.cn)("flex items-center p-6 pt-0",l),...r})}).displayName="CardFooter"},3702:function(e,t,l){"use strict";l.d(t,{c:function(){return r},q:function(){return a}});var s=l(2177);let r={Play:"Play",Id:"Id",Code:"Code",ListBullet:"ListBullet",Upload:"Upload",Clock:"Clock",Desktop:"Desktop"},a=e=>{switch(e){case r.Play:return s.o1U;case r.Id:return s.Xwj;case r.Code:return s.dNJ;case r.ListBullet:return s.jVc;case r.Upload:return s.rG2;case r.Clock:return s.T39;case r.Desktop:return s.ugZ;default:return}}}},function(e){e.O(0,[310,285,792,694,640,411,971,69,744],function(){return e(e.s=1645)}),_N_E=e.O()}]);