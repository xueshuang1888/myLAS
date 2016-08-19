package org.ofbiz.activiti;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.activiti.engine.ProcessEngine;
import org.activiti.engine.ProcessEngineConfiguration;
import org.activiti.engine.form.AbstractFormType;
import org.activiti.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.ofbiz.base.container.Container;
import org.ofbiz.base.container.ContainerConfig;
import org.ofbiz.base.container.ContainerException;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilProperties;

public class ActivitiContainer implements Container {
	
	private String name;
	
	private static final String module = ActivitiContainer.class.getName();
 
	private String dbFile = "activiti.db.properties";
	
	@Override
	public void init(String[] args, String name, String configFile)
			throws ContainerException {
		// TODO Auto-generated method stub
		this.name = name;

        ContainerConfig.Container cc = ContainerConfig.getContainer(name, configFile);
        
        Debug.logInfo("------activiti workflow start------", module);  
        //获取工作流数据库配置文件  
        Properties jdbc=UtilProperties.getProperties(dbFile);  
        //配置工作流引擎  
        ProcessEngineConfiguration pecfg = ProcessEngineConfiguration.createStandaloneProcessEngineConfiguration();  
        pecfg.setJdbcUsername(jdbc.getProperty("username"));  
        pecfg.setJdbcPassword(jdbc.getProperty("password"));  
        pecfg.setJdbcUrl(jdbc.getProperty("url"));  
        pecfg.setJdbcDriver(jdbc.getProperty("driver"));  
        pecfg.setLabelFontName("宋体");
        pecfg.setActivityFontName("宋体");
        
        //连接池设置  
        pecfg.setJdbcMaxActiveConnections(Integer.valueOf(jdbc.getProperty("jdbcMaxActiveConnections")));  
        pecfg.setJdbcMaxIdleConnections(Integer.valueOf(jdbc.getProperty("jdbcMaxIdleConnections")));  
        pecfg.setJdbcMaxCheckoutTime(Integer.valueOf(jdbc.getProperty("jdbcMaxCheckoutTime")));  
        pecfg.setJdbcMaxWaitTime(Integer.valueOf(jdbc.getProperty("jdbcMaxWaitTime")));  
  
        //设置启动检查数据表  
        pecfg.setDatabaseSchemaUpdate(ProcessEngineConfiguration.DB_SCHEMA_UPDATE_TRUE);  
        
        ProcessEngineConfigurationImpl processEngineConfigurationImpl = (ProcessEngineConfigurationImpl) pecfg;
        List<AbstractFormType> customFormTypes = new ArrayList<AbstractFormType>();
        customFormTypes.add(new UsersFormType());
        processEngineConfigurationImpl.setCustomFormTypes(customFormTypes);
        
        
  
        //创建表并获取流程引擎  
        ProcessEngine pe = processEngineConfigurationImpl.buildProcessEngine();  
        
        
        //todo 加载初始化流程  
  
        //初始化工作流引擎工厂  
        ActivitiProcessEngineFactory.init(pe,"default");  
        Debug.logInfo("------activiti workflow loaded------", module); 
	}

	@Override
	public boolean start() throws ContainerException {
		// TODO Auto-generated method stub
		Debug.log("activiti-start");  
        return true;  
	}

	@Override
	public void stop() throws ContainerException {
		// TODO Auto-generated method stub
		Debug.log("activiti-stop");  
	}

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return name;
	}

}
