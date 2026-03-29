import{a as q}from"./chunk-TJB7OVNW.js";import{a as H}from"./chunk-MDCTX52Q.js";import{a as J}from"./chunk-AXWH3ITT.js";import"./chunk-QWGTRFML.js";import"./chunk-H2GM34DQ.js";import"./chunk-4RZHSILW.js";import"./chunk-CH66Y72F.js";import"./chunk-GIMKSE3A.js";import"./chunk-O6TGXNT4.js";import"./chunk-SF5CY7VW.js";import"./chunk-7P2W45IN.js";import{k as Z,l as j}from"./chunk-HAO7WTGD.js";import"./chunk-RVWCJPGQ.js";import"./chunk-JFYS7ML4.js";import{Da as D,Ga as V,O,S as W,T as M,U as P,V as R,W as I,X as L,Y as N,Z as B,h as i,j as m,ka as U,v as G}from"./chunk-NSPFOU7F.js";import"./chunk-U3HIERX4.js";import{g as F}from"./chunk-7IQ7MVDA.js";var K=G.pie,w={sections:new Map,showData:!1,config:K},h=w.sections,y=w.showData,ne=structuredClone(K),se=i(()=>structuredClone(ne),"getConfig"),le=i(()=>{h=new Map,y=w.showData,W()},"clear"),ce=i(({label:e,value:t})=>{h.has(e)||(h.set(e,t),m.debug(`added new section: ${e}, with value: ${t}`))},"addSection"),de=i(()=>h,"getSections"),pe=i(e=>{y=e},"setShowData"),ge=i(()=>y,"getShowData"),Q={getConfig:se,clear:le,setDiagramTitle:L,getDiagramTitle:N,setAccTitle:M,getAccTitle:P,setAccDescription:R,getAccDescription:I,addSection:ce,getSections:de,setShowData:pe,getShowData:ge},fe=i((e,t)=>{H(e,t),t.setShowData(e.showData),e.sections.map(t.addSection)},"populateDb"),ue={parse:i(e=>F(null,null,function*(){let t=yield J("pie",e);m.debug(t),fe(t,Q)}),"parse")},me=i(e=>`
  .pieCircle{
    stroke: ${e.pieStrokeColor};
    stroke-width : ${e.pieStrokeWidth};
    opacity : ${e.pieOpacity};
  }
  .pieOuterCircle{
    stroke: ${e.pieOuterStrokeColor};
    stroke-width: ${e.pieOuterStrokeWidth};
    fill: none;
  }
  .pieTitleText {
    text-anchor: middle;
    font-size: ${e.pieTitleTextSize};
    fill: ${e.pieTitleTextColor};
    font-family: ${e.fontFamily};
  }
  .slice {
    font-family: ${e.fontFamily};
    fill: ${e.pieSectionTextColor};
    font-size:${e.pieSectionTextSize};
    // fill: white;
  }
  .legend text {
    fill: ${e.pieLegendTextColor};
    font-family: ${e.fontFamily};
    font-size: ${e.pieLegendTextSize};
  }
`,"getStyles"),he=me,Se=i(e=>{let t=[...e.entries()].map(o=>({label:o[0],value:o[1]})).sort((o,s)=>s.value-o.value);return V().value(o=>o.value)(t)},"createPieArcs"),ve=i((e,t,X,o)=>{m.debug(`rendering pie chart
`+e);let s=o.db,T=B(),$=j(s.getConfig(),T.pie),A=40,n=18,p=4,l=450,S=l,v=q(t),c=v.append("g");c.attr("transform","translate("+S/2+","+l/2+")");let{themeVariables:a}=T,[_]=Z(a.pieOuterStrokeWidth);_??=2;let E=$.textPosition,g=Math.min(S,l)/2-A,Y=D().innerRadius(0).outerRadius(g),ee=D().innerRadius(g*E).outerRadius(g*E);c.append("circle").attr("cx",0).attr("cy",0).attr("r",g+_/2).attr("class","pieOuterCircle");let b=s.getSections(),x=Se(b),te=[a.pie1,a.pie2,a.pie3,a.pie4,a.pie5,a.pie6,a.pie7,a.pie8,a.pie9,a.pie10,a.pie11,a.pie12],d=U(te);c.selectAll("mySlices").data(x).enter().append("path").attr("d",Y).attr("fill",r=>d(r.data.label)).attr("class","pieCircle");let k=0;b.forEach(r=>{k+=r}),c.selectAll("mySlices").data(x).enter().append("text").text(r=>(r.data.value/k*100).toFixed(0)+"%").attr("transform",r=>"translate("+ee.centroid(r)+")").style("text-anchor","middle").attr("class","slice"),c.append("text").text(s.getDiagramTitle()).attr("x",0).attr("y",-(l-50)/2).attr("class","pieTitleText");let C=c.selectAll(".legend").data(d.domain()).enter().append("g").attr("class","legend").attr("transform",(r,f)=>{let u=n+p,re=u*d.domain().length/2,ie=12*n,oe=f*u-re;return"translate("+ie+","+oe+")"});C.append("rect").attr("width",n).attr("height",n).style("fill",d).style("stroke",d),C.data(x).append("text").attr("x",n+p).attr("y",n-p).text(r=>{let{label:f,value:u}=r.data;return s.getShowData()?`${f} [${u}]`:f});let ae=Math.max(...C.selectAll("text").nodes().map(r=>r?.getBoundingClientRect().width??0)),z=S+A+n+p+ae;v.attr("viewBox",`0 0 ${z} ${l}`),O(v,l,z,$.useMaxWidth)},"draw"),xe={draw:ve},Ae={parser:ue,db:Q,renderer:xe,styles:he};export{Ae as diagram};
