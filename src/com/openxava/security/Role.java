package com.openxava.security;

import java.util.Map;

public interface Role {

	public String getRolename();

	public void setRolename(String rolename);

	public Map<String, String> getPermissions();

	public void setPermissions(Map<String, String> permissions);
}
