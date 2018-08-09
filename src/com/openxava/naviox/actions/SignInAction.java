package com.openxava.naviox.actions;

import org.openxava.util.*;
import com.openxava.naviox.impl.*;

/**
 * 
 * @author Javier Paniza 
 */
public class SignInAction extends ForwardToOriginalURIBaseAction {
	
	public void execute() throws Exception {		
		SignInHelper.init(getRequest(), getView()); 
		String userName = getView().getValueString("user");
		String password = getView().getValueString("password");
		String userType = getView().getValueString("userType");
		if (Is.emptyString(userName, password)) { 
			addError("unauthorized_user"); 
			return;
		}	
		boolean isAuthorized;
		if (userName.equalsIgnoreCase("admin")) {
			userType = "admin";
			isAuthorized = SignInHelper.isAuthorized(userName, password);
		} else {
			isAuthorized = SignInHelper.isAuthorized(userName, password, userType, getErrors());
		}
			
		if (!isAuthorized) {
			return;									
		}		
		SignInHelper.signIn(getRequest().getSession(), userName, userType);
		getView().reset();
		forwardToOriginalURI(); 
	}
	
}
