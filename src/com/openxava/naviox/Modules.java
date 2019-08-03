package com.openxava.naviox;

import java.io.*;
import java.util.*;
import java.util.prefs.*;

import javax.servlet.*;
import javax.servlet.http.*;

import org.apache.commons.logging.*;
import org.openxava.application.meta.*;
import org.openxava.util.*;

import com.openxava.naviox.impl.*;
import com.openxava.naviox.util.*;
import com.openxava.security.*;

/**
 * 
 * @author Javier Paniza
 */
public class Modules implements Serializable {

	public final static String FIRST_STEPS = "FirstSteps";

	private static Log log = LogFactory.getLog(Modules.class);
	private final static int MODULES_ON_TOP = 10;
	private final static int BOOKMARK_MODULES = 100;
	private final static ModuleComparator comparator = new ModuleComparator();
	private static String preferencesNodeName = null;
	private List<MetaModule> all;
	private HashMap<String, MetaModule> modules;
	private List<MetaModule> topModules = null;
	private List<MetaModule> bookmarkModules = null;
	private List<MetaModule> allowedModules = null;
	private MetaModule current;
	private int fixedModulesCount = 0; 

	public static void init(String applicationName) {
		MetaModuleFactory.setApplication(applicationName);
		DB.init();
		createFirstStepsModule(applicationName);
		ModulesHelper.init(applicationName);
	}

	private static void createFirstStepsModule(String applicationName) {
		MetaApplication app = MetaApplications.getMetaApplication(applicationName);
		MetaModule firstStepsModule = new MetaModule();
		firstStepsModule.setName(FIRST_STEPS);
		firstStepsModule.setModelName("SignIn"); // The model does not matter
		firstStepsModule.setWebViewURL("/naviox/firstSteps.jsp");
		firstStepsModule.setModeControllerName("Void");
		app.addMetaModule(firstStepsModule);
	}

	public void reset() {
		all = null;
		modules = null;
		allowedModules = null;
		topModules = null;
		bookmarkModules = null;
		current = null;
		if (!NaviOXPreferences.getInstance().isStartInLastVisitedModule()) {
			try {
				getPreferences().remove("current");
			} catch (BackingStoreException ex) {
				log.warn(XavaResources.getString("current_module_problem"), ex);
			}
		}
	}

	public boolean hasModules() {
		return (NaviOXPreferences.getInstance().isShowModulesMenuWhenNotLogged() || Users.getCurrent() != null)
				&& !getAll().isEmpty();
	}

	private MetaModule createWelcomeModule(MetaApplication app) {
		MetaModule result = new MetaModule();
		result.setName("Welcome");
		result.setWebViewURL("naviox/welcome");
		return result;
	}

	public void setCurrent(String application, String module, boolean retainOrder) { 
		this.current = MetaModuleFactory.create(application, module);
		if (topModules == null) loadTopModules();	
		if (!ModulesHelper.isPublic(this.current.getName())) {
			int idx = indexOf(topModules, current); 
			if (idx < 0) {
				if (topModules.size() >= MODULES_ON_TOP) {
					topModules.remove(topModules.size() - 1); 
				}				
				topModules.add(fixedModulesCount, current); 
			}		
			else if (!retainOrder && idx >= fixedModulesCount) { 
				topModules.remove(idx);
				topModules.add(fixedModulesCount, current); 
			}	
		}
		
		storeTopModules();
	}

	public boolean showsIndexLink() {
		return ModulesHelper.showsIndexLink();
	}

	public String getCurrent(HttpServletRequest request) {
		try {
			String current = ModulesHelper.getCurrent(request);
			return current == null ? getPreferences().get("current", FIRST_STEPS) : current;
		} catch (Exception ex) {
			log.warn(XavaResources.getString("current_module_problem"), ex);
			return FIRST_STEPS;
		}
	}

	public String getCurrentModuleDescription(HttpServletRequest request) {
		try {
			String organization = Organizations.getCurrentName(request);
			String prefix = organization == null ? "" : organization + " - ";
			String application = NaviOXPreferences.getInstance().isShowApplicationName()
					? current.getMetaApplication().getLabel() + " - "
					: "";
			return prefix + application + current.getLabel();
		} catch (Exception ex) {
			log.warn(XavaResources.getString("module_description_problem"), ex);
			return XavaResources.getString("unknow_module");
		}
	}

	public String getCurrentModuleName() {
		return current == null ? null : current.getName();
	}

	public void bookmarkCurrentModule() {
		if (bookmarkModules == null)
			loadBookmarkModules();
		int idx = indexOf(bookmarkModules, current);
		if (idx < 0) {
			bookmarkModules.add(current);
		}
		storeBookmarkModules();
	}

	public void unbookmarkCurrentModule() {
		if (bookmarkModules == null)
			loadBookmarkModules();
		int idx = indexOf(bookmarkModules, current);
		if (idx >= 0) {
			bookmarkModules.remove(idx);
		}
		storeBookmarkModules();
	}

	public boolean isCurrentBookmarked() {
		return isBookmarked(current);
	}

	public boolean isBookmarked(MetaModule module) {
		if (bookmarkModules == null)
			loadBookmarkModules();
		return indexOf(bookmarkModules, module) >= 0;
	}

	private void loadTopModules() {
		topModules = new ArrayList<MetaModule>();
		loadFixedModules(topModules);
		if (NaviOXPreferences.getInstance().isRememberVisitedModules()) {
			loadModulesFromPreferences(topModules, "", MODULES_ON_TOP);	
		}
	}
	
	private void loadFixedModules(Collection<MetaModule> modules) { 
		String fixedModules = NaviOXPreferences.getInstance().getFixModulesOnTopMenu();
		fixedModulesCount = 0; 
		if (Is.emptyString(fixedModules)) return;
		for (String moduleName: Strings.toCollection(fixedModules)) {
			if (loadModule(modules, moduleName)) fixedModulesCount++;												
		}
	}
	
	private boolean loadModule(Collection<MetaModule> modules, String moduleName) { 
		try {
			MetaModule module = MetaModuleFactory.create(moduleName);
			if (!modules.contains(module) && isModuleAuthorized(module)) { 
				modules.add(module);
				return true;
			}
		}
		catch (Exception ex) {					
			log.warn(XavaResources.getString("module_not_loaded", moduleName, MetaModuleFactory.getApplication()), ex);
		}
		return false;
	}

	private void loadBookmarkModules() {
		bookmarkModules = new ArrayList<MetaModule>();
		loadModulesFromPreferences(bookmarkModules, "bookmark.", BOOKMARK_MODULES);
	}

	private void loadModulesFromPreferences(List<MetaModule> modules, String prefix, int limit) {
		try {
			Preferences preferences = getPreferences();
			for (int i = 0; i < limit; i++) { 
				String applicationName = preferences.get(prefix + "application." + i, null);
				if (applicationName == null) break;
				String moduleName = preferences.get(prefix + "module." + i, null);
				if (moduleName == null) break;				
				loadModule(modules, moduleName); 
			}		
		}
		catch (Exception ex) {
			log.warn(XavaResources.getString("loading_modules_problem"), ex); 
		}
	}

	public boolean isModuleAuthorized(HttpServletRequest request) {
		try {
			if (request.getRequestURI().contains("module.jsp"))
				return false;
			if (Users.getCurrent() == null && request.getRequestURI().contains("/phone/"))
				return false;
			if (!(request.getRequestURI().startsWith(request.getContextPath() + "/m/")
					|| request.getRequestURI().startsWith(request.getContextPath() + "/p/")
					|| request.getRequestURI().startsWith(request.getContextPath() + "/modules/")))
				return true;
			String[] uri = request.getRequestURI().split("/");
			if (uri.length < 4)
				return false;
			return isModuleAuthorized(request, MetaModuleFactory.create(uri[1], uri[3]));
		} catch (Exception ex) {
			log.warn(XavaResources.getString("module_not_authorized"), ex);
			return false;
		}

	}

	/**
	 * @since 5.7
	 */
	public boolean isModuleAuthorized(String module) {
		return isModuleAuthorized(null, module);
	}

	/**
	 * @since 5.7
	 */
	public boolean isModuleAuthorized(HttpServletRequest request, String module) {
		try {
			return isModuleAuthorized(request, MetaModuleFactory.create(module));
		} catch (Exception ex) {
			log.warn(XavaResources.getString("module_not_authorized"), ex);
			return false;
		}

	}

	public String getModuleURI(HttpServletRequest request, String moduleName) {
		MetaModule module = getModuleByName(moduleName);
		if (module == null)
			return "#";
		String organization = Organizations.getCurrent(request);
		String prefix = organization == null ? "" : "/o/" + organization;
		return "/" + module.getMetaApplication().getName() + prefix + "/m/" + module.getName();
	}

	public MetaModule getModuleByName(String moduleName) {
		if (modules == null)
			return null;
		return modules.get(moduleName);
	}

	// public MetaModule getAllowedModeules(List allowedModules) {
	// if (modules == null)
	// return null;
	// return modules.get(moduleName);
	// }

	public String getModuleURI(HttpServletRequest request, MetaModule module) {
		String organization = Organizations.getCurrent(request);
		String prefix = organization == null ? "" : "/o/" + organization;
		return "/" + module.getMetaApplication().getName() + prefix + "/m/" + module.getName();
	}

	boolean isModuleAuthorized(MetaModule module) {
		return isModuleAuthorized(null, module);
	}

	private boolean isModuleAuthorized(HttpServletRequest request, MetaModule module) {
		return Collections.binarySearch(getAll(), module, comparator) >= 0;
	}

	private void storeTopModules() {
		storeModulesInPreferences(topModules, "", MODULES_ON_TOP, true);
	}

	private void storeBookmarkModules() {
		storeModulesInPreferences(bookmarkModules, "bookmark.", BOOKMARK_MODULES, false);
	}

	private void storeModulesInPreferences(Collection<MetaModule> modules, String prefix, int limit,
			boolean storeCurrent) {
		try {
			Preferences preferences = getPreferences();
			int i = 0;
			for (MetaModule module : modules) {
				preferences.put(prefix + "application." + i, module.getMetaApplication().getName());
				preferences.put(prefix + "module." + i, module.getName());
				i++;
			}
			for (; i < limit; i++) {
				preferences.remove(prefix + "application." + i);
				preferences.remove(prefix + "module." + i);
			}
			if (storeCurrent && !"SignIn".equals(current.getName())) {
				// Consultorio Juridico preferences.put("current", current.getName());
			}

			preferences.flush();
		} catch (Exception ex) {
			log.warn(XavaResources.getString("storing_modules_problem"), ex);
		} 
	}

	private Preferences getPreferences() throws BackingStoreException {
		return Users.getCurrentPreferences().node(getPreferencesNodeName());
	}

	private static String getPreferencesNodeName() {
		if (preferencesNodeName == null) {
			Collection<MetaApplication> apps = MetaApplications.getMetaApplications();
			for (MetaApplication app : apps) {
				preferencesNodeName = "naviox." + app.getName();
				break;
			}
			if (preferencesNodeName == null)
				preferencesNodeName = "naviox.UNKNOWN";
		}
		return preferencesNodeName;
	}

	public Collection getTopModules() {
		return topModules;
	}

	public Collection getBookmarkModules() {
		if (bookmarkModules == null)
			loadBookmarkModules();
		return bookmarkModules;
	}

	public Collection getAllowedModules() {
		if (allowedModules == null)
			allowedModules = loadAllowedModules();
		return allowedModules;

	}

	public List<MetaModule> loadAllowedModules() {
		List<MetaModule> allowedModules = new ArrayList<MetaModule>();
		if (Users.getCurrentUserInfo().getUserType() == null) {
			List<MetaModule> modules = new ArrayList<MetaModule>();
			return modules;
		}
		if (Users.getCurrentUserInfo().getUserType().equalsIgnoreCase("admin")) { 
			return all;
		}
		List<String> allowedSecurityModules = Security.getModulesAllowed();
		for (String module : allowedSecurityModules) {
			MetaModule metaModule = getModuleByName(module);
			allowedModules.add(metaModule);
		}
		return allowedModules;

	}

	public List getAll() {
		if (all == null) {
			all = ModulesHelper.getAll();
			Collections.sort(all, comparator);
		}
		if (all != null && all.size() > 0) {
			modules = new HashMap<String, MetaModule>();
			for (MetaModule module : all) {
				modules.put(module.getName(), module);
			}
		}
		if (allowedModules == null) {
			//Consultorio Juridico
			// allowedModules = ModulesHelper.getAllowedModules()
		}
		return all;
	}

	public String getUserAccessModule(ServletRequest request) {
		return ModulesHelper.getUserAccessModule(request);
	}

	private int indexOf(Collection<MetaModule> topModules, MetaModule current) {
		int idx = 0;
		for (MetaModule module : topModules) {
			if (module.getName().equals(current.getName())
					&& module.getMetaApplication().getName().equals(current.getMetaApplication().getName()))
				return idx;
			idx++;
		}
		return -1;
	}

	private static class ModuleComparator implements Comparator<MetaModule> {

		public int compare(MetaModule a, MetaModule b) {
			return a.getName().compareTo(b.getName());
		}

	}

	public HashMap<String, MetaModule> getModules() {
		return modules;
	}

	public void setModules(HashMap<String, MetaModule> modules) {
		this.modules = modules;
	}

}
