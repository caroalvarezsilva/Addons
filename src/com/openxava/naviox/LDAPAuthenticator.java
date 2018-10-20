package com.openxava.naviox;

import java.util.*;

import javax.naming.*;
import javax.naming.directory.*;

import com.openxava.naviox.util.*;

public class LDAPAuthenticator {

	public boolean authenticateUser(String username, String password, String userType) {
		
		String ldapURL = (String)NaviOXPreferences.getInstance().getLDAPConfiguration(userType).get("ldapURL");
		String ldapBase = (String)NaviOXPreferences.getInstance().getLDAPConfiguration(userType).get("ldapBase");
		String dn = "uid=" + username + "," + ldapBase;
		Hashtable<String, String> environment = new Hashtable<String, String>();
		environment.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
		environment.put(Context.PROVIDER_URL, ldapURL);
		environment.put(Context.SECURITY_AUTHENTICATION, "simple");
		environment.put(Context.SECURITY_PRINCIPAL, dn);
		environment.put(Context.SECURITY_CREDENTIALS, password);

		try {
			DirContext authContext = new InitialDirContext(environment);
			return true;

		} catch (AuthenticationException ex) {
			ex.printStackTrace();
			return false;
		}

		catch (NamingException ex) {
			ex.printStackTrace();
			return false;
		}
	}
}
