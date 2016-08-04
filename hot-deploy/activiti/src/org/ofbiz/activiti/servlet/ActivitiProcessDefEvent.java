package org.ofbiz.activiti.servlet;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;

import org.activiti.bpmn.converter.BpmnXMLConverter;
import org.activiti.bpmn.model.BpmnModel;
import org.activiti.editor.constants.ModelDataJsonConstants;
import org.activiti.editor.language.json.converter.BpmnJsonConverter;
import org.activiti.engine.FormService;
import org.activiti.engine.IdentityService;
import org.activiti.engine.ProcessEngineConfiguration;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.activiti.engine.impl.context.Context;
import org.activiti.engine.repository.Model;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.image.ProcessDiagramGenerator;
import org.apache.commons.lang3.StringUtils;
import org.ofbiz.activiti.ActivitiProcessEngineFactory;
import org.ofbiz.activiti.ProcessInstanceDiagramCmd;
import org.ofbiz.base.util.Debug;
import org.ofbiz.entity.GenericValue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class ActivitiProcessDefEvent {

	private static final String module = ActivitiProcessDefEvent.class.getName();
	/**
	 * 查看xml/image
	 * 
	 * @param request
	 * @param response
	 * @return
	 */
	public static String readResource(HttpServletRequest request,
			HttpServletResponse response) {
		String processDefinitionId = request
				.getParameter("processDefinitionId");
		String resourceType = request.getParameter("resourceType");
		RepositoryService repositoryService = ActivitiProcessEngineFactory
				.getRepositoryService();
		ProcessDefinition processDefinition = repositoryService
				.createProcessDefinitionQuery()
				.processDefinitionId(processDefinitionId).singleResult();
		String resourceName = "";
		if (resourceType.equals("image")) {
			resourceName = processDefinition.getDiagramResourceName();
		} else if (resourceType.equals("xml")) {
			resourceName = processDefinition.getResourceName();
		}
		InputStream resourceAsStream = repositoryService.getResourceAsStream(
				processDefinition.getDeploymentId(), resourceName);
		byte[] b = new byte[1024];
		int len = -1;

		try {
			response.reset();
			while ((len = resourceAsStream.read(b, 0, 1024)) != -1) {
				response.getOutputStream().write(b, 0, len);
			}
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}

		return "success";
	}

	/**
	 * 删除流程部署实例
	 * 
	 * @param request
	 * @param response
	 * @return
	 */
	public static String delActivitiProcess(HttpServletRequest request,
			HttpServletResponse response) {

		try {
			RepositoryService repositoryService = ActivitiProcessEngineFactory
					.getRepositoryService();
			String deploymentId = request.getParameter("deploymentId");
			repositoryService.deleteDeployment(deploymentId, true);

		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
			return "error";
		}

		return "success";
	}

	/**
	 * 将流程实例转为模型
	 * 
	 * @param request
	 * @param response
	 * @return
	 */
	public static String transeferToModel(HttpServletRequest request,
			HttpServletResponse response) {

		try {

			String processDefinitionId = request
					.getParameter("processDefinitionId");

			RepositoryService repositoryService = ActivitiProcessEngineFactory
					.getRepositoryService();
			ProcessDefinition processDefinition = repositoryService
					.createProcessDefinitionQuery()
					.processDefinitionId(processDefinitionId).singleResult();
			InputStream bpmnStream = repositoryService.getResourceAsStream(
					processDefinition.getDeploymentId(),
					processDefinition.getResourceName());
			XMLInputFactory xif = XMLInputFactory.newInstance();
			InputStreamReader in = new InputStreamReader(bpmnStream, "UTF-8");
			XMLStreamReader xtr = xif.createXMLStreamReader(in);
			BpmnModel bpmnModel = new BpmnXMLConverter()
					.convertToBpmnModel(xtr);

			BpmnJsonConverter converter = new BpmnJsonConverter();
			com.fasterxml.jackson.databind.node.ObjectNode modelNode = converter
					.convertToJson(bpmnModel);
			Model modelData = repositoryService.newModel();
			modelData.setKey(processDefinition.getKey());
			modelData.setName(processDefinition.getResourceName());
			modelData.setCategory(processDefinition.getDeploymentId());

			ObjectNode modelObjectNode = new ObjectMapper().createObjectNode();
			modelObjectNode.put(ModelDataJsonConstants.MODEL_NAME,
					processDefinition.getName());
			modelObjectNode.put(ModelDataJsonConstants.MODEL_REVISION, 1);
			modelObjectNode.put(ModelDataJsonConstants.MODEL_DESCRIPTION,
					processDefinition.getDescription());
			modelData.setMetaInfo(modelObjectNode.toString());

			repositoryService.saveModel(modelData);

			repositoryService.addModelEditorSource(modelData.getId(), modelNode
					.toString().getBytes("utf-8"));

			request.setAttribute("modelId", modelData.getId());
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}

		return "success";
	}

	/**
	 * 更新状态
	 * 
	 * @param state
	 * @param processDefinitionId
	 * @return
	 */
	public static String updateState(HttpServletRequest request,
			HttpServletResponse response) {
		RepositoryService repositoryService = ActivitiProcessEngineFactory
				.getRepositoryService();

		String state = request.getParameter("state");
		String processDefinitionId = request
				.getParameter("processDefinitionId");
		if (state.equals("active")) {
			repositoryService.activateProcessDefinitionById(
					processDefinitionId, true, null);
		} else if (state.equals("suspend")) {
			repositoryService.suspendProcessDefinitionById(processDefinitionId,
					true, null);
		}
		return "success";
	}

	/**
	 * 读取流程跟踪图
	 * 
	 * @param request
	 * @param response
	 * @return
	 */
	public static String readTraceImage(HttpServletRequest request,
			HttpServletResponse response) {
		String executionId = request.getParameter("executionId");
		
		RuntimeService runtimeService = ActivitiProcessEngineFactory
				.getRuntimeService();

		RepositoryService repositoryService = ActivitiProcessEngineFactory
				.getRepositoryService();

		ProcessInstance processInstance = runtimeService
				.createProcessInstanceQuery().processInstanceId(executionId)
				.singleResult();
		BpmnModel bpmnModel = repositoryService.getBpmnModel(processInstance
				.getProcessDefinitionId());
		List<String> activeActivityIds = runtimeService
				.getActiveActivityIds(executionId);
		//highLightedActivities highLightedFlows
		// 不使用spring请使用下面的两行代码
		// ProcessEngineImpl defaultProcessEngine = (ProcessEngineImpl)
		// ProcessEngines.getDefaultProcessEngine();
		// Context.setProcessEngineConfiguration(defaultProcessEngine.getProcessEngineConfiguration());

		// 使用spring注入引擎请使用下面的这行代码
		ProcessEngineConfiguration processEngineConfiguration = ActivitiProcessEngineFactory
				.getProcessEngine().getProcessEngineConfiguration();
		Context.setProcessEngineConfiguration((ProcessEngineConfigurationImpl) processEngineConfiguration);

		ProcessDiagramGenerator diagramGenerator = processEngineConfiguration
				.getProcessDiagramGenerator();
		/*InputStream imageStream = diagramGenerator.generateDiagram(bpmnModel,
				"png", activeActivityIds);
		diagramGenerator.generateDiagram(bpmnModel, "png",activeActivityIds,activeActivityIds, "宋体", "宋体", null,1.0);*/
		ProcessInstanceDiagramCmd processInstanceDiagramCmd = 
				new ProcessInstanceDiagramCmd(executionId, ActivitiProcessEngineFactory.getRuntimeService(), repositoryService, ActivitiProcessEngineFactory.getHistoryService());
		InputStream imageStream = processInstanceDiagramCmd.execute(null);
		

		// 输出资源内容到相应对象
		byte[] b = new byte[1024];
		int len;
		try {
			//request.setCharacterEncoding("UTF-8");
			
			response.reset();
			while ((len = imageStream.read(b, 0, 1024)) != -1) {
				response.getOutputStream().write(b, 0, len);
			}
			
			response.setCharacterEncoding("UTF-8");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return "success";
	}
	
	/**
	 * 启动流程
	 * @param request
	 * @param response
	 */
	public static String startProcess(HttpServletRequest request,HttpServletResponse response){
		Map<String, String> formProperties = new HashMap<String, String>();
		HttpSession session = request.getSession();
		String processDefinitionId = request.getParameter("processDefinitionId");

        // 从request中读取参数然后转换
        Map<String, String[]> parameterMap = request.getParameterMap();
        Set<Entry<String, String[]>> entrySet = parameterMap.entrySet();
        for (Entry<String, String[]> entry : entrySet) {
            String key = entry.getKey();

            // fp_的意思是form paremeter
            if (StringUtils.defaultString(key).startsWith("fp_")) {
                formProperties.put(key.split("_")[1], entry.getValue()[0]);
            }
        }

        Debug.logInfo("start form parameters: {}", module, formProperties);

        GenericValue userLogin = (GenericValue) session.getAttribute("userLogin");
        // 用户未登录不能操作，实际应用使用权限框架实现，例如Spring Security、Shiro等
       /* if (user == null || StringUtils.isBlank(user.getId())) {
            return "redirect:/login?timeout=true";
        }*/
        ProcessInstance processInstance = null;
        IdentityService identityService = ActivitiProcessEngineFactory.getIdentityService();
        FormService formService = ActivitiProcessEngineFactory.getFormService();
        
        try {
            identityService.setAuthenticatedUserId(userLogin.getString("userLoginId"));
            processInstance = formService.submitStartFormData(processDefinitionId, formProperties);
            Debug.logInfo("start a processinstance: {}",module, processInstance);
        } finally {
            identityService.setAuthenticatedUserId(null);
        }
		
        return "sucess";
	}
}
