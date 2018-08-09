package com.openxava.security;

import java.util.List;
import java.util.Map;

import com.openxava.naviox.model.*;

public interface UserType {

	public List<Role> getRoles();

	public void setRoles(List<Role> roles);

	public String getUserType();

	public void setUserType(String userType);
}
