function uiFactory(){
let n=0; const q=e=>"MyFirstModule."+e;
const appearance=()=>({$Type:"Pages$Appearance",class:"",style:"",dynamicClasses:"",designProperties:{}});
const base=t=>({$Type:t,name:"competitionWidget"+(++n),appearance:appearance(),tabIndex:0});
const no=()=>({$Type:"Pages$NoClientAction",disabledDuringExecution:true});
const fmt=(date=false)=>({$Type:"Pages$FormattingInfo",decimalPrecision:2,groupDigits:false,enumFormat:"Text",dateFormat:date?"DateTime":"Date",customDateFormat:""});
const attr=(e,a)=>({$Type:"DomainModels$AttributeRef",attribute:q(e)+"."+a});
const mf=m=>({$Type:"Pages$MicroflowClientAction",disabledDuringExecution:true,microflowSettings:{$Type:"Pages$MicroflowSettings",microflow:q(m),parameterMappings:[],progressBar:"Blocking",asynchronous:false,formValidations:"All",outputMappings:[]}});
const text=(s,mode="Paragraph")=>({...base("Pages$DynamicText"),"ct:content":s,renderMode:mode,nativeTextStyle:"Text"});
const dyn=(e,a,label="")=>({...base("Pages$DynamicText"),"ct:content":{$Type:"Pages$ClientTemplate","t:template":label+"{1}",parameters:[{$Type:"Pages$ClientTemplateParameter",attributeRef:attr(e,a),formattingInfo:fmt(a.endsWith("At"))}],"t:fallback":""},renderMode:"Paragraph",nativeTextStyle:"Text"});
const button=(label,m)=>({...base("Pages$ActionButton"),"ct:caption":label,"t:tooltip":"",action:mf(m),renderType:"Button",buttonStyle:"Primary",ariaRole:"Button"});
const cancel=()=>({...button("Cancel",""),action:{$Type:"Pages$ClosePageClientAction",disabledDuringExecution:true,numberOfPagesToClose:"1"},buttonStyle:"Default"});
const input=(e,a,label,type="String")=>{
const b={...base(type==="DateTime"?"Pages$DatePicker":type==="Boolean"?"Pages$CheckBox":type==="Enumeration"?"Pages$RadioButtonGroup":"Pages$TextBox"),"ct:labelTemplate":label,attributeRef:attr(e,a),validation:{$Type:"Pages$WidgetValidation","t:message":""},onChangeAction:no(),onEnterAction:no(),onLeaveAction:no(),editable:"Always",readOnlyStyle:"Inherit"};
if(type==="Boolean")Object.assign(b,{ariaRequired:false,labelPosition:"Default"});
else if(type==="Enumeration")b.renderHorizontal=false;
else if(type==="DateTime")b.formattingInfo=fmt(true);
else Object.assign(b,{"ct:placeholderTemplate":"",formattingInfo:fmt(),onEnterKeyPressAction:no(),maxLengthCode:-1,autoFocus:false,inputMask:"",isPasswordBox:false,keyboardType:"Default",submitBehaviour:"OnEndEditing",submitOnInputDelay:300});
return b;
};
const source=(e,param)=>({$Type:"Pages$DataViewSource",entityRef:{$Type:"DomainModels$DirectEntityRef",entity:q(e)},sourceVariable:{$Type:"Pages$PageVariable",pageParameter:param,useAllPages:false,subKey:""},forceFullObjects:false});
const sourceMF=m=>({$Type:"Pages$MicroflowSource",microflowSettings:mf(m).microflowSettings,forceFullObjects:false});
const view=(source,widgets)=>({...base("Pages$DataView"),dataSource:source,widgets,footerWidgets:[],"t:noEntityMessage":"No data is available.",editability:"Always",showFooter:false,labelWidth:3,readOnlyStyle:"Control"});
const lv=(e,widgets,xp="",m)=>({...base("Pages$ListView"),dataSource:m?sourceMF(m):{$Type:"Pages$ListViewXPathSource",entityRef:{$Type:"DomainModels$DirectEntityRef",entity:q(e)},sortBar:{$Type:"Pages$GridSortBar",sortItems:[]},search:{$Type:"Pages$ListViewSearch",searchRefs:[]},forceFullObjects:false,xPathConstraint:xp},widgets,templates:[],pageSize:50,editable:false,scrollDirection:"Vertical",numberOfColumns:1,clickAction:no(),pullDownAction:no()});
const tabs=items=>({...base("Pages$TabContainer"),tabPages:items.map(([name,widgets])=>({...base("Pages$TabPage"),"t:caption":name,widgets,refreshOnShow:true}))});
const page=(title,e,param,widgets)=>({title,layout:"Atlas_Core.Atlas_Default",parameters:e?[{$Type:"Pages$PageParameter",name:param,isRequired:true,parameterType:{$Type:"DataTypes$ObjectType",entity:q(e)}}]:[],variables:[],widgets:[{$Type:"Pages$Content",slot:"Main",widgets:[text(title,"H1"),...(e?[view(source(e,param),widgets)]:widgets)]}]});
const form=(title,fields,save,extras=[])=>page(title,"CompetitionForm","Form",[...fields.map(([a,label,type])=>input("CompetitionForm",a,label,type)),...extras,button("Save",save),cancel()]);
return {q,text,dyn,button,cancel,input,source,sourceMF,view,lv,tabs,page,form};
}
