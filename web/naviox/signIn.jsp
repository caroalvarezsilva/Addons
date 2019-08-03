<%
String app = request.getParameter("application");
%>

<div id="welcome">
	<jsp:include page='welcome.jsp'/>
</div>
<div id="sign_in_box">
	<jsp:include page='<%="../xava/module.jsp?application=" + app + "&module=SignIn"%>'/>
</div>
