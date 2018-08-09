package com.openxava.naviox.model;

import javax.persistence.*;
import org.openxava.annotations.*;

/**
 * 
 * @author Javier Paniza 
 */

public class SignIn {
	
	public enum UserType {
		student, teacher, publicOfficial
	};
	
	@Column(length=30) @LabelFormat(LabelFormatType.SMALL)
	private String user;

	@Column(length=30) @Stereotype("PASSWORD")
	@LabelFormat(LabelFormatType.SMALL)
	private String password;
	
	@Editor("ValidValuesVerticalRadioButton")
	@LabelFormat(LabelFormatType.NO_LABEL)
	private UserType userType;

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public String getUser() {
		return user;
	}

	public UserType getUserType() {
		return userType;
	}

	public void setUserType(UserType userType) {
		this.userType = userType;
	}


}
