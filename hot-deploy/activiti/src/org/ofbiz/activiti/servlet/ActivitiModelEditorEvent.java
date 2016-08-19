package org.ofbiz.activiti.servlet;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import javolution.util.FastMap;

import org.activiti.bpmn.converter.BpmnXMLConverter;
import org.activiti.bpmn.model.BpmnModel;
import org.activiti.editor.constants.ModelDataJsonConstants;
import org.activiti.editor.language.json.converter.BpmnJsonConverter;
import org.activiti.engine.ActivitiException;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.repository.Model;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.PNGTranscoder;
import org.apache.commons.lang3.StringUtils;
import org.ofbiz.activiti.ActivitiProcessEngineFactory;
import org.ofbiz.base.lang.JSON;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilValidate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class ActivitiModelEditorEvent {

	public static final String module = ActivitiModelEditorEvent.class.getName();
	private static final String MODEL_ID = "modelId";
	private static final String MODEL_NAME = "name";
	private static final String MODEL_REVISION = "revision";
	private static final String MODEL_DESCRIPTION = "description";
	
	/**
	 * 根据modelId获取activiti编辑模型
	 * @param request
	 * @param response
	 * @return
	 */
	public static String getEditorJson(HttpServletRequest request,
			HttpServletResponse response) {
		ObjectNode modelNode = null;
		ObjectMapper objectMapper = new ObjectMapper();
		
 		RepositoryService repositoryService = ActivitiProcessEngineFactory.getRepositoryService();
		String modelId = request.getParameter("modelId");
		Model model = repositoryService.getModel(modelId);
		String jsonStr = null;
		if (model != null) {
			try {
				if (UtilValidate.isNotEmpty(model.getMetaInfo())) {
					modelNode = (ObjectNode) objectMapper.readTree(model.getMetaInfo());
				} else {
					modelNode = objectMapper.createObjectNode();
					modelNode.put(MODEL_NAME, model.getName());
				}
				modelNode.put(MODEL_ID, model.getId());
				ObjectNode editorJsonNode = (ObjectNode) objectMapper
						.readTree(new String(repositoryService
								.getModelEditorSource(model.getId()), "utf-8"));
				modelNode.put("model", editorJsonNode);
				
				jsonStr = modelNode.toString();
			} catch (Exception e) {
				Debug.logWarning("Error creating model JSON", module);
				Debug.logError(e, module);
				//throw new ActivitiException("Error creating model JSON", e);
				return "error";
			}
		}
		
		try {
			writeJSONtoResponse(jsonStr, response);
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return "success";
	}
	
	/**
	 * 保存activiti模型
	 * @param request
	 * @param response
	 * @return
	 */
	public static String saveModel(HttpServletRequest request,
			HttpServletResponse response){
		String modelId = request.getParameter("modelId");
		String modelName = request.getParameter("name");
		String description = request.getParameter("description");
		String json_xml = request.getParameter("json_xml");
		String svg_xml = request.getParameter("svg_xml");
		
		RepositoryService repositoryService = ActivitiProcessEngineFactory.getRepositoryService();
		
		try {
			Model model = repositoryService.getModel(modelId);
			
			ObjectMapper objectMapper = new ObjectMapper();
			ObjectNode modelJson = (ObjectNode) objectMapper.readTree(model.getMetaInfo());
			 modelJson.put(MODEL_NAME, modelName);
		      modelJson.put(MODEL_DESCRIPTION, description);
		      model.setMetaInfo(modelJson.toString());
		      model.setName(modelName);
		      
		      repositoryService.saveModel(model);
		      
		      repositoryService.addModelEditorSource(model.getId(), json_xml.getBytes("utf-8"));
		      
		      InputStream svgStream = new ByteArrayInputStream(svg_xml.getBytes("utf-8"));
		      TranscoderInput input = new TranscoderInput(svgStream);
		      
		      PNGTranscoder transcoder = new PNGTranscoder();
		      // Setup output
		      ByteArrayOutputStream outStream = new ByteArrayOutputStream();
		      TranscoderOutput output = new TranscoderOutput(outStream);
		      
		      // Do the transformation
		      transcoder.transcode(input, output);
		      final byte[] result = outStream.toByteArray();
		      repositoryService.addModelEditorSourceExtra(model.getId(), result);
		      outStream.close();
		} catch (Exception e) {
			// TODO: handle exception
			Debug.logWarning("Error saving model", module);
			Debug.logError(e, module);
			//throw new ActivitiException("Error saving model", e);
			return "error";
		}
		return "success";
		
	}
	
	/**
	 * 创建模型
	 * @param request
	 * @param response
	 * @return
	 */
	public static String createModel(HttpServletRequest request,
			HttpServletResponse response){
		RepositoryService repositoryService = ActivitiProcessEngineFactory.getRepositoryService();
		String description = request.getParameter("description");
		String name = request.getParameter("name");
		String key = request.getParameter("key");
		String jsonStr = null;
		try {
            ObjectMapper objectMapper = new ObjectMapper();
            ObjectNode editorNode = objectMapper.createObjectNode();
            editorNode.put("id", "canvas");
            editorNode.put("resourceId", "canvas");
            ObjectNode stencilSetNode = objectMapper.createObjectNode();
            stencilSetNode.put("namespace", "http://b3mn.org/stencilset/bpmn2.0#");
            editorNode.put("stencilset", stencilSetNode);
            Model modelData = repositoryService.newModel();

            ObjectNode modelObjectNode = objectMapper.createObjectNode();
            modelObjectNode.put(ModelDataJsonConstants.MODEL_NAME, name);
            modelObjectNode.put(ModelDataJsonConstants.MODEL_REVISION, 1);
            description = StringUtils.defaultString(description);
            modelObjectNode.put(ModelDataJsonConstants.MODEL_DESCRIPTION, description);
            modelData.setMetaInfo(modelObjectNode.toString());
            modelData.setName(name);
            modelData.setKey(StringUtils.defaultString(key));

            repositoryService.saveModel(modelData);
            repositoryService.addModelEditorSource(modelData.getId(), editorNode.toString().getBytes("utf-8"));
            
            jsonStr = "{'modelId':'"+modelData.getId()+"'}";
            //response.sendRedirect(request.getContextPath() + "/modeler.html?modelId=" + modelData.getId());
            request.setAttribute("modelId", modelData.getId());
        } catch (Exception e) {
        	Debug.logWarning("Error saving model", module);
            Debug.logError("创建模型失败："+e, module);
            //return "error";
            jsonStr = "{'msg':'模型创建失败！'}";
        }
		
		/*try {
			writeJSONtoResponse(jsonStr, response);
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
		return "success";
	}
	
	public static String deployModel(HttpServletRequest request,
			HttpServletResponse response){
		RepositoryService repositoryService = ActivitiProcessEngineFactory.getRepositoryService();
		String modelId = request.getParameter("modelId");
		List<String> eventMessageList = new ArrayList<String>();
		try {
            Model modelData = repositoryService.getModel(modelId);
            ObjectNode modelNode = (ObjectNode) new ObjectMapper().readTree(repositoryService.getModelEditorSource(modelData.getId()));
            byte[] bpmnBytes = null;

            BpmnModel model = new BpmnJsonConverter().convertToBpmnModel(modelNode);
            bpmnBytes = new BpmnXMLConverter().convertToXML(model);

            String processName = modelData.getName() + ".bpmn20.xml";
            Deployment deployment = repositoryService.createDeployment().name(modelData.getName()).addString(processName, new String(bpmnBytes)).deploy();
            //redirectAttributes.addFlashAttribute("message", "部署成功，部署ID=" + deployment.getId());
            eventMessageList.add("部署成功，部署ID=" + deployment.getId());
            request.setAttribute("_EVENT_MESSAGE_LIST_ ", eventMessageList);
        } catch (Exception e) {
           // logger.error("根据模型部署流程失败：modelId={}", modelId, e);
            Debug.logWarning("Error saving model", module);
            Debug.logError("根据模型部署流程失败：modelId={}", module,modelId, e);
            return "error";
        }
		
		//"redirect:/workflow/model/list";
		return "success";
	}
	
	public static String deleteModel(HttpServletRequest request,
			HttpServletResponse response){
		List<String> eventMessageList = new ArrayList<String>();
		RepositoryService repositoryService = ActivitiProcessEngineFactory.getRepositoryService();
		String modelId = request.getParameter("modelId");
		try {
			repositoryService.deleteModel(modelId);
			eventMessageList.add("删除模型成功！");
			request.setAttribute("_EVENT_MESSAGE_LIST_ ", eventMessageList);
		} catch (Exception e) {
			// TODO: handle exception
			Debug.logWarning("删除流程模型失败！", module);
            Debug.logError("删除流程模型失败：modelId={}", module,modelId, e);
            return "error";
		}
		
		return "success";
	}
	
	
	private static void writeJSONtoResponse( String jsonStr, HttpServletResponse response) throws UnsupportedEncodingException {
        if (jsonStr == null) {
            Debug.logError("JSON Object was empty; fatal error!", module);
            return;
        }

        // set the JSON content type
        response.setContentType("application/json");
        // jsonStr.length is not reliable for unicode characters
        response.setContentLength(jsonStr.getBytes("UTF8").length);

        // return the JSON String
        Writer out;
        try {
            out = response.getWriter();
            out.write(jsonStr);
            out.flush();
        } catch (IOException e) {
            Debug.logError(e, module);
        }
    }
}
