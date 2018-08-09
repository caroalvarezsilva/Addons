package com.openxava.security;


import java.io.IOException;
import java.net.URL;

import com.openxava.security.xml.*;


public class SecurityManagerFactory {
  private final static String XML_SECURIYY_FILE = "security.xml";
  
  public static String TYPE_XML= "xml";
  public static SecurityManager buildSecurityManager (String type) throws SecurityException {
    
    SecurityManager securityManager=null;
    if (TYPE_XML.equalsIgnoreCase(type)) {
      securityManager = XMLSecurityManager.instance();
      URL url = SecurityManagerFactory.class.getClassLoader().getResource(XML_SECURIYY_FILE);
      try {
        securityManager.init(url.openStream());
      } catch (IOException e) {
        throw new SecurityException(e);
      }
    } else {
      throw new SecurityException("Invalid security type " + type);
    }
    return securityManager;
  }

}
