<%@ taglib uri='/tlds/struts-template.tld' prefix='template' %>
<template:insert template='/templates/page.jsp'>
  <template:put name='title' content='Active User Sessions' direct='true'/>
  <template:put name='content' content='/content/_sessions.jsp'/>
</template:insert>