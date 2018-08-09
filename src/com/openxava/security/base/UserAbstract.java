package com.openxava.security.base;

import java.util.List;
import java.util.Map;

import com.openxava.naviox.model.*;
import com.openxava.security.*;

public abstract class UserAbstract implements UserType {

	private String userType;
	private List<Role> roles;

	
	@Override
	public String getUserType() {
		return userType;
	}
	@Override
	public void setUserType(String userType) {
		this.userType = userType;
	}

	@Override
	public List<Role> getRoles() {
		return roles;
	}

	@Override
	public void setRoles(List<Role> roles) {
		this.roles = roles;
	}

}
