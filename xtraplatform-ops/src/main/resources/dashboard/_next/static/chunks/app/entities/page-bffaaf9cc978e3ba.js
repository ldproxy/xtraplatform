(self.webpackChunk_N_E=self.webpackChunk_N_E||[]).push([[146],{5531:function(e,t,a){Promise.resolve().then(a.bind(a,3813))},3813:function(e,t,a){"use strict";a.r(t),a.d(t,{default:function(){return v}});var i=a(3827),n=a(4747),l=a(5744),s=a(7907),c=a(2169),r=a(4090),o=a(6162),u=a(3702),d=a(8403);function v(){let[e,t]=(0,r.useState)([]),[a,v]=(0,r.useState)("overview"),f=(0,s.useRouter)(),h=(0,s.usePathname)(),p=e.map(d.RL).filter((e,t,a)=>a.indexOf(e)===t),m=e.reduce((e,t)=>{let a=(0,d.RL)(t);return e[a]||(e[a]=0),e[a]++,e},{}),g=(0,d.zM)(e,p),x=async()=>{try{let e=await (0,c.fi)(),a=await (0,c.fL)();e.forEach(e=>{let t=a.find(t=>t.name==="entities/".concat(e.type,"/").concat(e.id));e.status=t&&t.state?t.state:"UNKNOWN"}),t(e)}catch(e){console.error("Error loading entities:",e)}};return(0,r.useEffect)(()=>{let e=setInterval(()=>{x()},o.gi);return h&&v(window.location.hash.slice(1)||"overview"),()=>clearInterval(e)},[h]),o.Dh&&(console.log("entityTypeStatusCounts:",g),console.log("entities",e),console.log("Counts:",m.API),console.log("entityTypes",p)),(0,i.jsxs)("div",{className:"flex-1 space-y-4 p-8 pt-0",children:[(0,i.jsx)("div",{className:"flex items-center justify-between space-y-2",children:(0,i.jsx)("h2",{className:"text-2xl font-semibold tracking-tight",children:"Entities"})}),(0,i.jsxs)(l.mQ,{value:a,onValueChange:e=>{v(e),f.push("".concat(h,"#").concat(e))},className:"h-full space-y-6",children:[(0,i.jsx)("div",{className:"space-between flex items-center",children:(0,i.jsxs)(l.dr,{children:[(0,i.jsx)(l.SP,{value:"overview",children:(0,i.jsx)("span",{children:"Overview"})}),p.map(e=>(0,i.jsx)(l.SP,{value:e,children:(0,i.jsx)("span",{children:(0,d.Xd)(e)})},e))]})}),(0,i.jsx)(l.nU,{value:"overview",children:(0,i.jsx)("div",{className:"grid gap-4 md:grid-cols-2 lg:grid-cols-4",children:p.map(e=>(0,i.jsx)(n.Z,{main:(0,d.Xd)(e),footer:(0,d.Ph)(g[e]),total:m[e],onClick:()=>v(e),route:"".concat(h,"#").concat(e),Icon:(0,u.q)("Id")},e))})}),p.map(t=>(0,i.jsx)(l.nU,{value:t,children:(0,i.jsx)("div",{className:"grid gap-4 md:grid-cols-2 lg:grid-cols-4",children:e.filter(e=>(0,d.RL)(e)===t).map(e=>(0,i.jsx)(n.Z,{header:e.status,main:e.id,footer:e.subType.toUpperCase(),route:"/entities/details?id=".concat(e.uid)},e.uid))})},t))]})]})}},8403:function(e,t,a){"use strict";a.d(t,{Ph:function(){return c},RL:function(){return i},Xd:function(){return n},lY:function(){return l},zM:function(){return s}});let i=e=>"services"===e.type?"API":e.subType.split(/[/]/)[0],n=e=>e[0].toUpperCase()+e.substring(1)+"s",l=e=>e.reduce((e,t)=>(i(t),e||(e={available:0,limited:0,unavailable:0}),"AVAILABLE"===t.status?e.available++:"LIMITED"===t.status?e.limited++:"UNAVAILABLE"===t.status&&e.unavailable++,e),{available:0,limited:0,unavailable:0}),s=(e,t)=>t.reduce((t,a)=>(t[a]=l(e.filter(e=>i(e)===a)),t),{}),c=e=>{if(!e||!e)return"No entities found";let t="";return e.available&&(t+="".concat(e.available," available")),e.limited&&(t.length>0&&(t+=" "),t+="".concat(e.limited," limited")),e.unavailable&&(t.length>0&&(t+=" "),t+="".concat(e.unavailable," unavailable")),t}}},function(e){e.O(0,[310,658,792,594,617,971,69,744],function(){return e(e.s=5531)}),_N_E=e.O()}]);