package org.ofbiz.activiti.servlet;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import javolution.util.FastMap;

import org.activiti.engine.ActivitiException;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.repository.Model;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.PNGTranscoder;
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
				Debug.logInfo("Error creating model JSON", module);
				Debug.logError(e, module);
				throw new ActivitiException("Error creating model JSON", e);
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
			Debug.logInfo("Error saving model", module);
			Debug.logError(e, module);
			throw new ActivitiException("Error saving model", e);
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
