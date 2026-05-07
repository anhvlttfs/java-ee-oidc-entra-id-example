<%@page import="Models.Objects.User"%>
<% User u = (User) request.getSession().getAttribute("loggedUser");%>
<div class="container">
    <header class="border-bottom lh-1 py-3">
        <div class="row flex-nowrap justify-content-between align-items-center">
            <div class="col-4 pt-1">
                <a class="link-secondary" target="_blank" href="#">View on GitHub</a>
            </div>
            <div class="col-4 text-center">
                <a class="blog-header-logo text-body-emphasis text-decoration-none" href="<%= request.getContextPath()%>/">Entra External ID Example</a>
            </div>
            <div class="col-4 d-flex justify-content-end align-items-center">
                <% if (u == null) {%>
                <a href="<%= request.getContextPath()%>/auth?action=signin" class="btn btn-sm btn-outline-secondary">Login</a>
                <% } else {%>
                <a href="<%= request.getContextPath()%>/auth?action=logout" class="btn btn-sm btn-outline-secondary">Logout</a>
                <% } %>
            </div>
        </div>
    </header>
</div>
<br>