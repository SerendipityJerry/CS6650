import Model.Message;
import Model.Swipe;
import java.io.PrintWriter;
import java.util.stream.Collectors;
import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.*;
import java.io.IOException;
import com.google.gson.Gson;

@WebServlet(name = "SwipeServlet", value = "/SwipeServlet")
public class SwipeServlet extends HttpServlet {

    private static final Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {

    }


    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
        throws IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        String urlPath = request.getPathInfo();
        Message message = new Message();
        String resJson;

        //  Returns the object of PrintWriter Class, in which print(String args) method is declared to print
        //  anything on the browser's page as a response.
        PrintWriter out = response.getWriter();

        if (urlPath == null || urlPath.isEmpty() || !validateUrl(urlPath)) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            message.setMessage("Invalid URL");
            resJson = gson.toJson(message);
            out.write(resJson);
            out.flush();
            return;
        }

        String reqBody = request.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
        // fromJson()方法来实现从Json字符串转化为到java实体的方法，提供两个参数，分别是json字符串以及需要转换对象的类型。
        Swipe swipe = gson.fromJson(reqBody, Swipe.class);
        if (!validateSwipe(swipe)) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            message.setMessage("Invalid Inputs");
        } else {
            message.setMessage("Write Successfully");
            response.setStatus(HttpServletResponse.SC_CREATED);
        }

        resJson = gson.toJson(message);
        out.write(resJson);
        out.flush();
    }

    private boolean validateUrl(String url) {
        url = url.replace("/", "");
        return url.equals("left") || url.equals("right");
    }

    private boolean validateSwipe(Swipe swipe) {
        if (swipe.getSwiper() == null || swipe.getSwipee() == null || swipe.getComment() == null) {
            return false;
        }
        int swiper = Integer.parseInt(swipe.getSwiper());
        int swipee = Integer.parseInt(swipe.getSwipee());
        return swiper >= 1 && swiper <= 5000 && swipee >= 1 && swipee <= 1000000
            && swipe.getComment().length() <= 256;
    }
}
