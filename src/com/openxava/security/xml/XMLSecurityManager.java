package com.openxava.security.xml;

import java.io.*;
import java.util.*;

import javax.xml.parsers.*;

import org.openxava.util.*;
import org.w3c.dom.*;
import org.xml.sax.*;

import com.openxava.security.*;
import com.openxava.security.SecurityException;
import com.openxava.security.SecurityManager;

public class XMLSecurityManager implements SecurityManager {

	private static final String TAG_USERTYPE = "userType";
	private static final String TAG_USERTYPENAME = "userTypeName";
	private static final String TAG_ROLE = "role";

	private static final String ATT_USERNAME = "username";
	private static final String ATT_PASSWORD = "password";
	private static final String ATT_FULLNAME = "fullname";
	private static final String ATT_ROLES = "roles";
	private static final String ATT_ROLENAME = "rolename";
	private static final String ATT_OBJECT = "object";
	private static final String ATT_ACTION = "action";

	private static XMLSecurityManager instance = null;
	private Map<String, Role> roles = new HashMap<String, Role>();
	private Map<String, UserType> usersType = new HashMap<String, UserType>();
	private Map<String, Map<String, String>> userPermissions = new HashMap<String, Map<String, String>>();

	private XMLSecurityManager() {
	}

	public static XMLSecurityManager instance() {
		if (instance == null)
			instance = new XMLSecurityManager();
		return instance;
	};

	@Override
	public void init(Object securitySource) throws SecurityException {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setValidating(false);
		DocumentBuilder db;
		try {
			db = dbf.newDocumentBuilder();
			db.setEntityResolver(new EntityResolver() {
				@Override
				public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
					return null;
				}
			});
			Document root = db.parse((InputStream) securitySource);
			loadRoles(root);
			loadUsersType(root);
		} catch (ParserConfigurationException | SAXException | IOException e) {
			throw new SecurityException(e);
		}
	}

	private void loadRoles(Document root) {
		NodeList nl = root.getElementsByTagName(TAG_ROLE);
		for (int i = 0; i < nl.getLength(); i++) {
			Node node = nl.item(i);
			NamedNodeMap nnm = node.getAttributes();
			if (nnm == null)
				continue;
			Node nodAux = nnm.getNamedItem(ATT_ROLENAME);
			if (nodAux == null)
				continue;
			String rolename = nodAux.getNodeValue();
			if (Is.empty(rolename))
				continue;
			Role role = new XMLRole();
			role.setRolename(rolename);
			Map<String, String> permissions = new HashMap<String, String>();
			role.setPermissions(permissions);
			roles.put(rolename, role);

			// Load permissions
			NodeList children = node.getChildNodes();
			if (children == null)
				continue;
			for (int j = 0; j < children.getLength(); j++) {
				nnm = children.item(j).getAttributes();
				if (nnm == null)
					continue;
				nodAux = nnm.getNamedItem(ATT_OBJECT);
				if (nodAux == null)
					continue;
				String object = nodAux.getNodeValue();
				nodAux = nnm.getNamedItem(ATT_ACTION);
				if (nodAux == null)
					continue;
				String action = nodAux.getNodeValue();
				if (Is.empty(object) || Is.empty(action))
					continue;
				permissions.put(object, action);
			}
		}

	}

	private void loadUsersType(Document root) {
		NodeList nl = root.getElementsByTagName(TAG_USERTYPE);
		for (int i = 0; i < nl.getLength(); i++) {
			Node node = nl.item(i);
			NamedNodeMap nnm = node.getAttributes();
			if (nnm == null)
				continue;
			Node nodAux = nnm.getNamedItem(TAG_USERTYPENAME);
			if (nodAux == null)
				continue;
			String userTypeName = nodAux.getNodeValue();
			if (Is.empty(userTypeName))
				continue;

			UserType userType = new XMLUser();
		
			usersType.put(userTypeName, userType);
			List<Role> lstRoles = new ArrayList<Role>();
			userType.setRoles(lstRoles);

			
			nodAux = nnm.getNamedItem(ATT_ROLES);
			if (nodAux == null)
				continue;
			String listroles = nodAux.getNodeValue();
			if (Is.empty(listroles))
				continue;
			String[] arrRoles = listroles.split(",");
			for (String role : arrRoles) {
				Role r = roles.get(role.trim());
				if (r != null) {
					lstRoles.add(r);
				}
			}
		}
	}

	public UserType getUserType(String userTypeName) {
		if (Is.empty(userTypeName))
			return null;
		UserType userType = usersType.get(userTypeName);
		return userType;

	}

	private Map<String, String> loadPermissions(UserType userType) {
		if (userType == null)
			return new HashMap<String, String>();
		Map<String, String> permissions = new HashMap<String, String>();
		if (Is.empty(userType.getRoles()))
			return new HashMap<String, String>();
		for (Role role : userType.getRoles()) {
			if (Is.empty(role.getPermissions()))
				continue;
			permissions.putAll(role.getPermissions());
		}
		return permissions;
	}

	@Override
	public boolean hasPermission(String userTypeName, String object, String action) {
		if (Is.empty(userTypeName))
			return false;
		Map<String, String> permissions = userPermissions.get(userTypeName);
		if (permissions == null) {
			UserType userType = getUserType(userTypeName);
			permissions = loadPermissions(userType);
		}
		if (permissions == null || permissions.size() == 0)
			return false;
		String act = permissions.get(object);
		if (Is.empty(act))
			return false;
		return act.equals(action);
	}

	@Override
	public boolean hasRole(String userTypeName, String rolename) {
		UserType userType = getUserType(userTypeName);
		if (userType == null)
			return false;
		return userType.getRoles().stream().anyMatch(r -> r.getRolename().equals(rolename));
	}

	@Override
	public boolean hasAnyObject(String userTypeName, String... objects) {
		if (Is.empty(userTypeName))
			return false;
		if (Is.emptyString(objects))
			return false;
		Map<String, String> permissions = userPermissions.get(userTypeName);
		if (permissions == null) {
			UserType userType = getUserType(userTypeName);
			permissions = loadPermissions(userType);
		}
		if (permissions == null || permissions.size() == 0)
			return false;
		List<String> lstObjects = Arrays.asList(objects);
		return permissions.keySet().stream().anyMatch(r -> lstObjects.contains(r));
	}
	
	@Override
	public List<String> getModulesAllowed(String userTypeName) {
		Map<String, String> permissions = new HashMap<String, String>();
		if (Is.empty(userTypeName))
			return null;
		permissions = userPermissions.get(userTypeName);
		if (permissions == null) {
			UserType userType = getUserType(userTypeName);
			permissions = loadPermissions(userType);
		}
		return new ArrayList<String>(permissions.keySet());
	}

}
