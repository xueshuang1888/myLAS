package org.ofbiz.activiti.servlet;

import java.io.InputStream;
import java.io.InputStreamReader;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;

import org.activiti.bpmn.converter.BpmnXMLConverter;
import org.activiti.bpmn.model.BpmnModel;
import org.activiti.editor.constants.ModelDataJsonConstants;
import org.activiti.editor.language.json.converter.BpmnJsonConverter;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.repository.Model;
import org.activiti.engine.repository.ProcessDefinition;
import org.ofbiz.activiti.ActivitiProcessEngineFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class ActivitiProcessDefEvent {

	/**
	 * 查看xml/image
	 * @param request
	 * @param response
	 * @return
	 */
	public static String readResource(HttpServletRequest request,
			HttpServletResponse response){
		String processDefinitionId = request.getParameter("processDefinitionId");
		String resourceType = request.getParameter("resourceType");
		RepositoryService repositoryService = ActivitiProcessEngineFactory.getRepositoryService();
		ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().processDefinitionId(processDefinitionId).singleResult();
        String resourceName = "";
        if (resourceType.equals("image")) {
            resourceName = processDefinition.getDiagramResourceName();
        } else if (resourceType.equals("xml")) {
            resourceName = processDefinition.getResourceName();
        }
        InputStream resourceAsStream = repositoryService.getResourceAsStream(processDefinition.getDeploymentId(), resourceName);
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
	 * @param request
	 * @param response
	 * @return
	 */
	public static String delActivitiProcess(HttpServletRequest request,HttpServletResponse response){
		
		try {
			RepositoryService repositoryService = ActivitiProcessEngineFactory.getRepositoryService();
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
	 * @param request
	 * @param response
	 * @return
	 */
	public static String transeferToModel(HttpServletRequest request,HttpServletResponse response){
		
		try {
			
			String processDefinitionId = request.getParameter("processDefinitionId");
			
			RepositoryService repositoryService = ActivitiProcessEngineFactory.getRepositoryService();
			ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
					.processDefinitionId(processDefinitionId).singleResult();
			InputStream bpmnStream = repositoryService.getResourceAsStream(processDefinition.getDeploymentId(),
					processDefinition.getResourceName());
			XMLInputFactory xif = XMLInputFactory.newInstance();
			InputStreamReader in = new InputStreamReader(bpmnStream, "UTF-8");
			XMLStreamReader xtr = xif.createXMLStreamReader(in);
			BpmnModel bpmnModel = new BpmnXMLConverter().convertToBpmnModel(xtr);
			
			BpmnJsonConverter converter = new BpmnJsonConverter();
			com.fasterxml.jackson.databind.node.ObjectNode modelNode = converter.convertToJson(bpmnModel);
			Model modelData = repositoryService.newModel();
			modelData.setKey(processDefinition.getKey());
			modelData.setName(processDefinition.getResourceName());
			modelData.setCategory(processDefinition.getDeploymentId());
			
			ObjectNode modelObjectNode = new ObjectMapper().createObjectNode();
			modelObjectNode.put(ModelDataJsonConstants.MODEL_NAME, processDefinition.getName());
			modelObjectNode.put(ModelDataJsonConstants.MODEL_REVISION, 1);
			modelObjectNode.put(ModelDataJsonConstants.MODEL_DESCRIPTION, processDefinition.getDescription());
			modelData.setMetaInfo(modelObjectNode.toString());
			
			repositoryService.saveModel(modelData);
			
			repositoryService.addModelEditorSource(modelData.getId(), modelNode.toString().getBytes("utf-8"));
			
			request.setAttribute("modelId", modelData.getId());
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
        
        return "success";
	}
	
	/**
	 * 更新状态
	 * @param state
	 * @param processDefinitionId
	 * @return
	 */
	public static String updateState(HttpServletRequest request,HttpServletResponse response) {
		RepositoryService repositoryService = ActivitiProcessEngineFactory
				.getRepositoryService();
		
		String state = request.getParameter("state");
		String processDefinitionId = request.getParameter("processDefinitionId");
		if (state.equals("active")) {
			repositoryService.activateProcessDefinitionById(
					processDefinitionId, true, null);
		} else if (state.equals("suspend")) {
			repositoryService.suspendProcessDefinitionById(processDefinitionId,
					true, null);
		}
		return "success";
	}
}
