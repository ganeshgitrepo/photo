<%@ page import="net.spy.photo.Category" %>
<%@ page import="net.spy.photo.Comment" %>
<%@ page import="net.spy.photo.PhotoSessionData" %>
<%@ page import="net.spy.photo.PhotoSearchResults" %>
<%@ taglib uri='/tlds/struts-template.tld' prefix='template' %>
<%@ taglib uri='/tlds/struts-logic.tld' prefix='logic' %>
<%@ taglib uri='/tlds/struts-html.tld' prefix='html' %>
<%@ taglib uri='/tlds/photo.tld' prefix='photo' %>

<%
	String imageId=request.getParameter("id");
	if(imageId==null) {
		imageId=request.getParameter("image_id");
	}
	String searchId=request.getParameter("search_id");
%>

<h1>ADMIN DISPLAY</h1>

<photo:getImage imageId="<%= imageId %>" searchId="<%= searchId %>">

	<table width="100%">
		<tr valign="top">
			<td align="left" width="10%">
				<photo:imgLink id="<%= 0 %>" relative="prev" searchId="<%= searchId %>">
					<photo:imgsrc alt="<<<" border="0" url="/images/l_arrow.gif"/>
				</photo:imgLink>
			</td>

			<td align="center">
				<div class="displayBrief"><%= image.getDescr() %></div>
			</td>

			<td align="right" width="10%">
				<photo:imgLink id="<%= 0 %>" relative="next" searchId="<%= searchId %>">
					<photo:imgsrc alt=">>>" border="0" url="/images/r_arrow.gif"/>
				</photo:imgLink>
			</td>
		</tr>
	</table>

	<div align="center">
		<photo:imgSrc id="<%= "" + image.getId() %>"
			width="<%= "" + image.getScaledDims().getWidth() %>"
			height="<%= "" + image.getScaledDims().getHeight() %>"
			scale="true"/>
	</div>

	<html:form action="/admeditimage">
		<html:hidden property="id" value="<%= "" + image.getId() %>"/>

	<p>
		<b>Category</b>:
		<html:select property="category"
			value="<%= "" + image.getCatId() %>" size="5">

			<photo:getCatList showAddable="true">
				<logic:iterate id="i" name="catList"> 
					<% Category category=(Category)i; %>
					<html:option value="<%= "" + category.getId() %>">
						<%= category.getName() %></html:option>
				</logic:iterate>
			</photo:getCatList>
		</html:select>

		<br/>
		<b>Keywords</b>:
			<html:text property="keywords" value="<%= image.getKeywords() %>"/><br>
		<b>Size</b>:  <%= image.getDimensions() %>
			(<%= image.getSize() %> bytes)<br>
		<b>Taken</b>:  <html:text property="taken" value="<%= image.getTaken() %>"/>
			<b>Added</b>:
			<%= image.getTimestamp() %>
		by <%= image.getAddedBy() %><br>
		<b>Info</b>:
		<html:textarea cols="60" rows="5" property="info"
			value="<%= image.getDescr() %>"/>

	</p>

	<html:submit>Save Info</html:submit>
	<html:reset>Restore to Original</html:reset>

	</html:form>

[<a href="logview.jsp?id=<%= "" + image.getId() %>"> Who's seen this?</a>] |
[<photo:imgLink id='<%= "" + image.getId() %>'>Linkable image</photo:imgLink>] |
[<a href="PhotoServlet?photo_id=<%= "" + image.getId() %>">Full
Size Image</a>]


<p class="comments">

	<h1>Comments</h1>

	<logic:iterate id="i"
		collection="<%= Comment.getCommentsForPhoto(image.getId()) %>">

		<% Comment comment=(Comment)i; %>

		<table class="comments" width="100%">
			<tr class="comment_header">
				<td>At <%= comment.getTimestamp() %>,
					<%= comment.getUser().getRealname() %> said the
					following:
				</td>
			</tr>
			<tr class="comment_body">
				<td>
					<%= comment.getNote() %>
				</td>
			</tr>
		</table>
	</logic:iterate>

</p>

<p>

	Submit a comment:<br/>

	<photo:guest negate="true">
		<html:form action="/addcomment">
			<html:errors/>
			<html:hidden property="imageId" value="<%= "" + image.getId() %>"/>
			<html:textarea property="comment" cols="50" rows="2"/>
			<br/>
			<html:submit>Add Comment</html:submit>
		</html:form>
	</photo:guest>

<p>

</p>

</photo:getImage>