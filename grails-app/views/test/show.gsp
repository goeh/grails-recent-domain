<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head><title>test page</title></head>

<body>
<h1>TestEntity ${testEntity}</h1>
<ol>
    <g:each in="${list.reverse()}" var="m">
        <li>${m?.encodeAsHTML()}</li>
    </g:each>
</ol>
</body>
</html>