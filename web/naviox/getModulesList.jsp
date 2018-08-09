<%@page import="com.openxava.security.Security"%>
<% 
if ("true".equals(request.getParameter("bookmarkModules"))) {
	modulesList = modules.getBookmarkModules();
	bookmarkModules = true;
}
else {
	modulesList = modules.getAllowedModules();
}
%>