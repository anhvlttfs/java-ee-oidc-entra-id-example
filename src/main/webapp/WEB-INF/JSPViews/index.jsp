<%@page import="java.util.Map"%>
<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html lang="en" data-bs-theme="auto">
    <head>
        <title>The Tech News - Home Page</title>
        <%@include file="/WEB-INF/JSPViews/global/htmlHead.jsp" %>
    </head>
    <body>
        <%@include file="/WEB-INF/JSPViews/global/pageHeader.jsp" %>

        <%
            User user = (User) session.getAttribute("loggedUser");
            String issuer = (String) session.getAttribute("issuer_oidc");
            Map<String, Object> claims = (Map<String, Object>) session.getAttribute("oidc_claims");
        %>
        <br>
        <main class="container">
            <div class="row mb-2">
                <% if (user != null) {%>
                <h1 class="text-body-emphasis">All claims</h1>
                <h2 class="text-body-emphasis">Common claims</h2>
                <p>
                    Preferred User name: <%= u.getPreferred_username()%>
                    <br>
                    Registered email: <%= u.getEmail()%>
                    <br>
                    Full name: <%= u.getName()%>
                    <br>
                    Country: <%= u.getCountry()%>
                    <br>
                    Issuer: <%= issuer%>
                </p>
                <% } else { %>
                <h1 class="text-body-emphasis">You are not logged in</h1>
                <% }%>

            </div>
        </main>
        <%@include file="/WEB-INF/JSPViews/global/footer.jsp" %>
        <%@include file="/WEB-INF/JSPViews/global/htmlScripts.jsp" %>
    </body>
</html>
