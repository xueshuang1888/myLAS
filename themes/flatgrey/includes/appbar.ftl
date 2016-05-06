<#--
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
-->

<#if (requestAttributes.externalLoginKey)??><#assign externalKeyParam = "?externalLoginKey=" + requestAttributes.externalLoginKey!></#if>
<#if (externalLoginKey)??><#assign externalKeyParam = "?externalLoginKey=" + requestAttributes.externalLoginKey!></#if>
<#assign ofbizServerName = application.getAttribute("_serverId")?default("default-server")>
<#assign contextPath = request.getContextPath()>
<#assign displayApps = Static["org.ofbiz.webapp.control.LoginWorker"].getAppBarWebInfos(security, userLogin, ofbizServerName, "main")>
<#assign displaySecondaryApps = Static["org.ofbiz.webapp.control.LoginWorker"].getAppBarWebInfos(security, userLogin, ofbizServerName, "secondary")>
<div>
	<script type="text/javascript">
		 
		$(document).ready(function(){
			$("#secondaryAppBarLi").on("mouseover",function(){
				$($(this).find("ul")[0]).show();
			});
			
			$("#secondaryAppBarLi").on("mouseout",function(){
				$($(this).find("ul")[0]).hide();
			}); 
		});
		
	</script>
</div>
<#if userLogin?has_content>
  <div id="main-navigation">
    <ul>
      <#assign appCount = 0>
      <#assign firstApp = true>
	  
			<#list displayApps as display>
				<#assign thisApp = display.getContextRoot()>
				<#assign selected = false>
				<#if thisApp == contextPath || contextPath + "/" == thisApp>
				  <#assign selected = true>
				</#if>
				<#assign servletPath = Static["org.ofbiz.webapp.WebAppUtil"].getControlServletPath(display)>
				<#assign thisURL = StringUtil.wrapString(servletPath)>
				<#if thisApp != "/">
				  <#assign thisURL = thisURL + "main">
				</#if>
				<#if layoutSettings.suppressTab?? && display.name == layoutSettings.suppressTab>
				  <#-- do not display this component-->
				<#else>
				<li>
				  <a href="${thisURL}${StringUtil.wrapString(externalKeyParam)}"<#if selected> class="selected"</#if><#if uiLabelMap??> title="${uiLabelMap[display.description]}">${uiLabelMap[display.title]}<#else> title="${display.description}">${display.title}</#if></a>
				</li>
				</#if>
			</#list>
		
	  <li id="secondaryAppBarLi">
		<a href="javascript:void(0);">其他</a>
		<ul class="secondary">
			<#list displaySecondaryApps as display>
				<#assign thisApp = display.getContextRoot()>
				<#assign selected = false>
				<#if thisApp == contextPath || contextPath + "/" == thisApp>
				  <#assign selected = true>
				</#if>
				  <#assign servletPath = Static["org.ofbiz.webapp.WebAppUtil"].getControlServletPath(display)>
				  <#assign thisURL = StringUtil.wrapString(servletPath)>
				  <#if thisApp != "/">
					<#assign thisURL = thisURL + "main">
				  </#if>
				   <li>
					<a href="${thisURL}${StringUtil.wrapString(externalKeyParam)}"<#if selected> class="selected"</#if><#if uiLabelMap??> title="${uiLabelMap[display.description]}">${uiLabelMap[display.title]}<#else> title="${display.description}">${display.title}</#if></a>
				   </li>
			</#list>
		</ul>
	  </li>
	  
	  
    </ul>
  </div>
</#if>
<div style="margin-top:2px;margin-left:0px;">
