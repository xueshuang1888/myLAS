package org.ofbiz.activiti;

import java.util.List;

import org.activiti.engine.form.AbstractFormType;
import org.ofbiz.base.util.StringUtil;

public class UsersFormType extends AbstractFormType {

    @Override
    public String getName() {
        return "users";
    }

    @Override
    public Object convertFormValueToModelValue(String propertyValue) {
        List<String> split = StringUtil.split(propertyValue, ",");
        return split;
    }

    @Override
    public String convertModelValueToFormValue(Object modelValue) {
    	
        return modelValue == null ? "" : modelValue.toString();
    }

}
