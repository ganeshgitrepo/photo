<%@ page import="javax.servlet.http.*" %>
<%@ page import="net.spy.photo.*" %>
<%@ taglib uri='/tlds/struts-logic.tld' prefix='logic' %>
<%@ taglib uri='/tlds/struts-html.tld' prefix='html' %>
<%@ taglib uri='/tlds/photo.tld' prefix='photo' %>

<p>

<table width="100%" border="1">

<tr>
	<th>User</th><th>Login Time</th><th>Idle(s)</th>
</tr>

<logic:iterate id="i" collection="<%= SessionWatcher.listSessions() %>">
	<% HttpSession ses=(HttpSession)i; %>
	<% PhotoSessionData sessionData=
		(PhotoSessionData)ses.getAttribute("photoSession"); %>
	<% PhotoUser user=sessionData.getUser(); %>

	<tr>
		<td><%= user.getRealname() %> (<%= user.getUsername() %>)</td>
		<td><%= new java.util.Date(ses.getCreationTime()) %></td>
		<td><%= (double)(System.currentTimeMillis()
			- ses.getLastAccessedTime())/1000.0 %></td>
	</tr>

</logic:iterate>

</table>

</p>