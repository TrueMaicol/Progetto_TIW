package filters;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

public class UserLogged implements Filter {
    public void init(FilterConfig config) throws ServletException {
    }

    public void destroy() {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws ServletException, IOException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;
        String loginPath = req.getServletContext().getContextPath() + "/index.html";
        HttpSession s = req.getSession();

        if(s.isNew() || s.getAttribute("user") == null) {
            res.setStatus(HttpServletResponse.SC_FORBIDDEN);
            //res.setHeader("Location", loginPath);
            res.sendRedirect(loginPath);
            return;
        }
        chain.doFilter(request, response);
    }
}
