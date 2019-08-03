package com.openxava.security;

import java.util.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openxava.util.Users;
import org.openxava.util.XavaResources;

import com.openxava.naviox.util.NaviOXPreferences;


public class Security {
  
  public final static String OBJ_ALL = "all";
  public final static String OBJ_DEPARTMENT = "Department";
  public final static String OBJ_AGENDAREQUEST = "AgendaRequest";
  public final static String OBJ_CASEFILE = "CaseFile";
  public final static String OBJ_ORDERS = "Orders";
  public final static String OBJ_PRODUCTLINES = "Productlines";
  public final static String OBJ_PRODUCTS = "Products";
  public final static String OBJ_PAYMENTS = "Payments";

  public final static String ACT_ALL = "all";
  public final static String ACT_HIDE_IMAGE = "hide-image";
  
  private final static String PREFERENCES_FILE = "naviox.properties";
  private static Log log = LogFactory.getLog(Security.class);
  
  private static SecurityManager securityManager;
  
  public static String getCurrentUser() {
    return Users.getCurrent();
  }
  
  public static String getCurrentUserType() {
	    return Users.getCurrentUserInfo().getUserType();
	  }
  
  private static SecurityManager getSecurityManager() {
    if (securityManager == null) {
      String type = NaviOXPreferences.getInstance().getSecurityManagerFactoryType();
      try {
        securityManager = SecurityManagerFactory.buildSecurityManager(type);
      } catch (SecurityException e) {
        log.error(XavaResources.getString("properties_file_error",
            PREFERENCES_FILE), e);
      }
    }
    return securityManager;
  }
  
  public static boolean  hasPermission(String object, String action) {
    SecurityManager sm = getSecurityManager();
    if (sm==null) return false;
    return sm.hasPermission(getCurrentUser(), object, action);
  }

  public static boolean  hasRole(String role) {
    SecurityManager sm = getSecurityManager();
    if (sm==null) return false;
    return sm.hasRole(getCurrentUser(), role);
  }
  
  public static boolean hasAnyObject(String ... objects ) {
    SecurityManager sm = getSecurityManager();
    if (sm==null) return false;
    return sm.hasAnyObject(getCurrentUserType(), objects);
  }
  
  public static List<String> getModulesAllowed() {
	  SecurityManager sm = getSecurityManager();
	  	Map<String, String> permissions = new HashMap<String, String>();
	    if (sm==null) return new ArrayList();
	    return sm.getModulesAllowed(getCurrentUserType());
  }
  


}
