<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html lang="en">
    <head>
        <title>Entra External ID Sample</title>
        <%@include file="/WEB-INF/JSPViews/global/htmlHead.jsp" %>
    </head>
    <body>
        <%@include file="/WEB-INF/JSPViews/global/pageHeader.jsp" %>
        <div class="container col-xl-10 col-xxl-8 px-4 py-5">
            <div class="row align-items-center g-lg-5 py-5">
                <div class="col-lg-7 text-center text-lg-start">
                    <h1 class="display-4 fw-bold lh-1 text-body-emphasis mb-3">Sign into this application from Entra External ID</h1>
                </div>
                <div class="col-md-10 mx-auto col-lg-5">
                    <a class="w-100 btn btn-lg btn-primary" href="<%= request.getContextPath() + "/auth?action=oidc_signin"%>">Sign in with SSO</a>
                </div>
            </div>
        </div>
        <%@include file="/WEB-INF/JSPViews/global/footer.jsp" %>
        <%@include file="/WEB-INF/JSPViews/global/htmlScripts.jsp" %>
    </body>
</html>
