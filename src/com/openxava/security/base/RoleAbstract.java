package com.openxava.security.base;

import java.util.Map;

import com.openxava.security.*;

public abstract class RoleAbstract implements Role {

	private String rolename;
	private Map<String, String> permissions;
 
	@Override
	public String getRolename() {
		return rolename;
	}

	@Override
	public void setRolename(String rolename) {
		this.rolename = rolename;
	}

	@Override
	public Map<String, String> getPermissions() {
		return permissions;
	}

	@Override
	public void setPermissions(Map<String, String> permissions) {
		this.permissions = permissions;
	}

}
