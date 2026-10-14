function factory(){
const q=n=>n.includes(".")?n:"MyFirstModule."+n;
const at=(e,n,v)=>({$Type:"Microflows$MemberChange",attribute:q(e)+"."+n,value:v});
const as=(n,v)=>({$Type:"Microflows$MemberChange",association:q(n),value:v});
const create=(e,out,items=[])=>({$Type:"Microflows$CreateObjectAction",entity:q(e),outputVariableName:out,items,refreshInClient:false});
const change=(v,items)=>({$Type:"Microflows$ChangeObjectAction",changeVariableName:v,items,refreshInClient:true});
const cv=(n,t,v)=>({$Type:"Microflows$CreateVariableAction",variableName:n,variableType:t,initialValue:v});
const sv=(n,v)=>({$Type:"Microflows$ChangeVariableAction",changeVariableName:n,value:v});
const ret=(e,out,xp="",one=true)=>({$Type:"Microflows$RetrieveAction",byDatabaseQuery:{entity:q(e),xPathConstraint:xp,takeOnlyFirst:one},outputVariableName:out});
const by=(v,a,out)=>({$Type:"Microflows$RetrieveAction",byAssociation:{startVariableName:v,association:q(a)},outputVariableName:out});
const call=(n,args={},out)=>({$Type:"Microflows$MicroflowCallAction",microflowCall:{$Type:"Microflows$MicroflowCall",microflow:q(n),parameterMappings:Object.entries(args).map(([k,v])=>({$Type:"Microflows$MicroflowCallParameterMapping",parameter:q(n)+"."+k,argument:v}))},...(out?{outputVariableName:out}:{useReturnVariable:false,outputVariableName:""})});
const commit=v=>({$Type:"Microflows$CommitAction",commitVariableName:v,refreshInClient:true});
const list=(e,n)=>({$Type:"Microflows$CreateListAction",entity:q(e),outputVariableName:n});
const add=(n,v)=>({$Type:"Microflows$ChangeListAction",changeVariableName:n,value:v});
const filter=(l,exp,out,find=false)=>({$Type:"Microflows$ListOperationAction",operation:{$Type:find?"Microflows$FindByExpression":"Microflows$FilterByExpression",listVariableName:l,expression:exp},outputVariableName:out});
const sort=(l,e,a,out)=>({$Type:"Microflows$ListOperationAction",operation:{$Type:"Microflows$Sort",listVariableName:l,sortItemList:{$Type:"Microflows$SortItemList",items:[{$Type:"Microflows$SortItem",attributeRef:{$Type:"DomainModels$AttributeRef",attribute:q(e)+"."+a}}]}},outputVariableName:out});
const msg=t=>({$Type:"Microflows$ShowMessageAction",template:{text:t}});
const show=(page,e,n)=>({$Type:"Microflows$ShowPageAction",pageSettings:{$Type:"Pages$PageSettings",page:q(page),parameterMappings:e?[{$Type:"Pages$PageParameterMapping",parameter:q(page)+"."+e,argument:"$"+n}]:[]}});
const end=v=>({end:v||""});
const iff=(condition,yes,no=[])=>({condition,yes,no});
const guard=(condition,message)=>({guard:condition,message});
const loop=(l,v,steps)=>({loop:{listVariableName:l,variableName:v},steps});
const whiledo=(condition,steps)=>({while:condition,steps});
const set=(path,value)=>({path,operation:{type:"set",value}});
function outline(spec){
 let def=[],edges=[],ctr=0;
 function node(type,x,y,extra={},props={}){const key=ctr++;return {key,type,x,y,shell:{$Type:type,x,y,...extra},props}}
 const params=(spec.params||[]).map((p,i)=>node("Microflows$MicroflowParameterObject",100,80-i*100,{name:p[0],type:p[2]|| (p[1].includes(".")?"Object":p[1]),...(p[1].includes(".")?{entity:p[1]}:{}),name:p[0]}));
 function chain(steps,baseY,startX=100,root=false){
 let nodes=[],previous=null,cursor=startX;
 const put=(n)=>{nodes.push(n);if(previous)edges.push({from:previous.key,to:n.key});previous=n;return n};
 if(root)put(node("Microflows$StartEvent",cursor,baseY)); 
 for(const s of steps){
  cursor+=200;
  if(s.guard){const n=put(node("Microflows$ExclusiveSplit",cursor,baseY,{}, {splitCondition:{$Type:"Microflows$ExpressionSplitCondition",expression:s.guard}}));
   let tail;if(s.message){tail=node("Microflows$ActionActivity",cursor,baseY+200,{}, {action:msg(s.message)});nodes.push(tail);edges.push({from:n.key,to:tail.key,caseValue:"false"});}
   const e=node("Microflows$EndEvent",cursor,baseY+(tail?360:200),{}, {returnValue:spec.returnType&&spec.returnType!=="Void"?(spec.returnType==="Boolean"?"false":spec.returnType==="Object"?"empty":spec.returnType==="String"?"''":"0"):""});nodes.push(e);edges.push({from:tail?tail.key:n.key,to:e.key,...(!tail?{caseValue:"false"}:{})});previous=n;previous.nextCase="true";
  }else if(s.condition){
   const n=put(node("Microflows$ExclusiveSplit",cursor,baseY,{}, {splitCondition:{$Type:"Microflows$ExpressionSplitCondition",expression:s.condition}}));
   const a=chain(s.yes,baseY,cursor+200,false),b=chain(s.no,baseY+200,cursor+200,false);
   if(!a.nodes.length){a.nodes.push(node("Microflows$ExclusiveMerge",cursor+200,baseY));a.first=a.last=a.nodes[0]}
   if(!b.nodes.length){b.nodes.push(node("Microflows$ExclusiveMerge",cursor,baseY+200));b.first=b.last=b.nodes[0]}
   nodes.push(...a.nodes,...b.nodes);edges.push({from:n.key,to:a.first.key,caseValue:"true"},{from:n.key,to:b.first.key,caseValue:"false"});
   cursor=Math.max(a.cursor,b.cursor)+200;
   const m=node("Microflows$ExclusiveMerge",cursor,baseY);nodes.push(m);edges.push({from:a.last.key,to:m.key},{from:b.last.key,to:m.key});previous=m;
  }else if(s.loop||s.while){
   const body=chain(s.steps,140,0,false);
   const width=body.cursor+130,height=Math.max(...body.nodes.map(n=>n.y+(n.type==="Microflows$LoopedActivity"?n.shell.size.height/2:40)))+80;
   const n=node("Microflows$LoopedActivity",cursor+width/2,baseY,{size:{width,height},objects:body.nodes.map(n=>n.shell)}, {loopSource:s.loop?{$Type:"Microflows$IterableList",...s.loop}:{$Type:"Microflows$WhileLoopCondition",whileExpression:s.while,caption:"Repeat"}});
   n.children=body.nodes;put(n);cursor=n.x+width/2;
  }else if("end"in s)put(node("Microflows$EndEvent",cursor,baseY,{}, {returnValue:s.end}));
  else put(node("Microflows$ActionActivity",cursor,baseY,{}, {action:s}));
 }
 return {nodes,first:nodes[0],last:previous,cursor};
 }
 const main=chain(spec.steps,300,100,true),all=[...params,...main.nodes];
 function collect(ns){for(const n of ns){def.push(n);if(n.children)collect(n.children)}}collect(all);
 for(const e of edges){const n=def.find(n=>n.key===e.from);if(n.nextCase&&!e.caseValue)e.caseValue=n.nextCase;}
 return {spec,nodes:all,def,edges,content:{$Type:"Microflows$Microflow",name:spec.name,objects:all.map(n=>n.shell),...(spec.returnType?{returnType:spec.returnType}:{}),...(spec.returnEntity?{returnTypeEntity:q(spec.returnEntity)}:{})}};
}
function configure(plan,doc){
 const paths=new Map();
 function rec(ns,objs,path){for(const n of ns){const i=objs.findIndex(o=>o.$Type===n.type&&o.relativeMiddlePoint?.x===n.x&&o.relativeMiddlePoint?.y===n.y);if(i<0)throw Error("Missing node "+n.type+" "+n.x+" "+n.y);const p=path+"/"+i;paths.set(n.key,p);if(n.children)rec(n.children,objs[i].objectCollection.objects,p+"/objectCollection/objects");}}
 rec(plan.nodes,doc.objectCollection.objects,"/objectCollection/objects");
 let ops=plan.def.flatMap(n=>Object.entries(n.props).filter(([k])=>k!=="name").map(([k,v])=>set(paths.get(n.key)+"/"+k,v)));
 ops.push(...plan.edges.map(e=>{const a=plan.def.find(n=>n.key===e.from),b=plan.def.find(n=>n.key===e.to);return {path:"/flows",operation:{type:"add",value:{$Type:"Microflows$SequenceFlow",originId:"$id("+paths.get(e.from)+")",destinationId:"$id("+paths.get(e.to)+")",originConnectionSide:Math.abs(b.y-a.y)>Math.abs(b.x-a.x)?"Bottom":"Right",destinationConnectionSide:Math.abs(b.y-a.y)>Math.abs(b.x-a.x)?"Top":"Left",...(e.caseValue?{caseValue:{enumerationCase:e.caseValue}}:{})}}}}));
 if(plan.spec.roles)ops.push(...plan.spec.roles.map(role=>({path:"/allowedModuleRoles",operation:{type:"add",value:q(role)}})));
 ops.push(set("/documentation",plan.spec.documentation||"Competition workflow. Mutations use a private transaction claim and server-side scope checks."));
 return ops;
}
return {q,at,as,create,change,cv,sv,ret,by,call,commit,list,add,filter,sort,msg,show,end,iff,guard,loop,whiledo,set,outline,configure};
}
