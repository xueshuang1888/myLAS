package org.ofbiz.activiti;

import java.util.concurrent.ConcurrentHashMap;

import org.activiti.engine.FormService;
import org.activiti.engine.HistoryService;
import org.activiti.engine.IdentityService;
import org.activiti.engine.ProcessEngine;
import org.activiti.engine.ProcessEngineConfiguration;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.ofbiz.base.lang.Factory;
import org.ofbiz.base.util.Debug;

public abstract class ActivitiProcessEngineFactory implements Factory<ProcessEngine, String> {
	
	public static final String module = ActivitiProcessEngineFactory.class.getName();  
    private static ProcessEngineConfiguration pecfg = null;  
    private static final ConcurrentHashMap<String, ProcessEngine> processEngineCache = new ConcurrentHashMap<String, ProcessEngine>();  
  
    public static ProcessEngine getProcessEngine(String processEngineName){  
  
        if (processEngineName == null) {  
            processEngineName = "default";  
        }  
        ProcessEngine processEngine = processEngineCache.get(processEngineName);  
        if (processEngine == null) {  
            Debug.logInfo("没有找到工作流引擎", module);  
        }  
        return processEngine;  
    }  
    public static ProcessEngine getProcessEngine(){  
        return getProcessEngine("default");  
    }  
    public static RepositoryService getRepositoryService(String processEngineName){  
        return getProcessEngine(processEngineName).getRepositoryService();  
    }  
    public static RepositoryService getRepositoryService(){  
        return getRepositoryService("default");  
    }  
    
    public static RuntimeService getRuntimeService(){
    	
    	return getProcessEngine().getRuntimeService();
    }
    
    public static IdentityService getIdentityService(){
    	
    	return getProcessEngine().getIdentityService();
    }
    
    public static FormService getFormService(){
    	return getProcessEngine().getFormService();
    }
    
    public static HistoryService getHistoryService(){
    	return getProcessEngine().getHistoryService();
    }
    
    public static void init(ProcessEngine pe,String name){  
        ActivitiProcessEngineFactory.processEngineCache.put(name,pe);  
    }  

}
