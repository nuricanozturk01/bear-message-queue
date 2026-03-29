import{a as A}from"./chunk-TJB7OVNW.js";import{a as E}from"./chunk-MDCTX52Q.js";import{a as _}from"./chunk-AXWH3ITT.js";import"./chunk-QWGTRFML.js";import"./chunk-H2GM34DQ.js";import"./chunk-4RZHSILW.js";import"./chunk-CH66Y72F.js";import"./chunk-GIMKSE3A.js";import"./chunk-O6TGXNT4.js";import"./chunk-SF5CY7VW.js";import"./chunk-7P2W45IN.js";import{l as x}from"./chunk-HAO7WTGD.js";import"./chunk-RVWCJPGQ.js";import"./chunk-JFYS7ML4.js";import{D as w,O as B,S,T as F,U as z,V as P,W,X as T,Y as D,h as n,j as v,v as y}from"./chunk-NSPFOU7F.js";import"./chunk-U3HIERX4.js";import{a as m,g as $}from"./chunk-7IQ7MVDA.js";var N={packet:[]},C=structuredClone(N),I=y.packet,M=n(()=>{let t=x(m(m({},I),w().packet));return t.showBits&&(t.paddingY+=10),t},"getConfig"),O=n(()=>C.packet,"getPacket"),G=n(t=>{t.length>0&&C.packet.push(t)},"pushWord"),H=n(()=>{S(),C=structuredClone(N)},"clear"),h={pushWord:G,getPacket:O,getConfig:M,clear:H,setAccTitle:F,getAccTitle:z,setDiagramTitle:T,getDiagramTitle:D,getAccDescription:W,setAccDescription:P},K=1e4,R=n(t=>{E(t,h);let e=-1,o=[],i=1,{bitsPerRow:s}=h.getConfig();for(let{start:a,end:r,label:p}of t.blocks){if(r&&r<a)throw new Error(`Packet block ${a} - ${r} is invalid. End must be greater than start.`);if(a!==e+1)throw new Error(`Packet block ${a} - ${r??a} is not contiguous. It should start from ${e+1}.`);for(e=r??a,v.debug(`Packet block ${a} - ${e} with label ${p}`);o.length<=s+1&&h.getPacket().length<K;){let[b,c]=U({start:a,end:r,label:p},i,s);if(o.push(b),b.end+1===i*s&&(h.pushWord(o),o=[],i++),!c)break;({start:a,end:r,label:p}=c)}}h.pushWord(o)},"populate"),U=n((t,e,o)=>{if(t.end===void 0&&(t.end=t.start),t.start>t.end)throw new Error(`Block start ${t.start} is greater than block end ${t.end}.`);return t.end+1<=e*o?[t,void 0]:[{start:t.start,end:e*o-1,label:t.label},{start:e*o,end:t.end,label:t.label}]},"getNextFittingBlock"),X={parse:n(t=>$(null,null,function*(){let e=yield _("packet",t);v.debug(e),R(e)}),"parse")},j=n((t,e,o,i)=>{let s=i.db,a=s.getConfig(),{rowHeight:r,paddingY:p,bitWidth:b,bitsPerRow:c}=a,u=s.getPacket(),l=s.getDiagramTitle(),g=r+p,d=g*(u.length+1)-(l?0:r),k=b*c+2,f=A(e);f.attr("viewbox",`0 0 ${k} ${d}`),B(f,d,k,a.useMaxWidth);for(let[L,Y]of u.entries())q(f,Y,L,a);f.append("text").text(l).attr("x",k/2).attr("y",d-g/2).attr("dominant-baseline","middle").attr("text-anchor","middle").attr("class","packetTitle")},"draw"),q=n((t,e,o,{rowHeight:i,paddingX:s,paddingY:a,bitWidth:r,bitsPerRow:p,showBits:b})=>{let c=t.append("g"),u=o*(i+a)+a;for(let l of e){let g=l.start%p*r+1,d=(l.end-l.start+1)*r-s;if(c.append("rect").attr("x",g).attr("y",u).attr("width",d).attr("height",i).attr("class","packetBlock"),c.append("text").attr("x",g+d/2).attr("y",u+i/2).attr("class","packetLabel").attr("dominant-baseline","middle").attr("text-anchor","middle").text(l.label),!b)continue;let k=l.end===l.start,f=u-2;c.append("text").attr("x",g+(k?d/2:0)).attr("y",f).attr("class","packetByte start").attr("dominant-baseline","auto").attr("text-anchor",k?"middle":"start").text(l.start),k||c.append("text").attr("x",g+d).attr("y",f).attr("class","packetByte end").attr("dominant-baseline","auto").attr("text-anchor","end").text(l.end)}},"drawWord"),J={draw:j},Q={byteFontSize:"10px",startByteColor:"black",endByteColor:"black",labelColor:"black",labelFontSize:"12px",titleColor:"black",titleFontSize:"14px",blockStrokeColor:"black",blockStrokeWidth:"1",blockFillColor:"#efefef"},V=n(({packet:t}={})=>{let e=x(Q,t);return`
	.packetByte {
		font-size: ${e.byteFontSize};
	}
	.packetByte.start {
		fill: ${e.startByteColor};
	}
	.packetByte.end {
		fill: ${e.endByteColor};
	}
	.packetLabel {
		fill: ${e.labelColor};
		font-size: ${e.labelFontSize};
	}
	.packetTitle {
		fill: ${e.titleColor};
		font-size: ${e.titleFontSize};
	}
	.packetBlock {
		stroke: ${e.blockStrokeColor};
		stroke-width: ${e.blockStrokeWidth};
		fill: ${e.blockFillColor};
	}
	`},"styles"),ot={parser:X,db:h,renderer:J,styles:V};export{ot as diagram};
