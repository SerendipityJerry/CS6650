
import Model.ChannelPool;
import Model.Message;
import Model.SwipeEvent;
import java.io.PrintWriter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.*;
import java.io.IOException;
import com.google.gson.Gson;
import com.rabbitmq.client.Channel;

@WebServlet(name = "SwipeServlet", value = "/SwipeServlet")
public class SwipeServlet extends HttpServlet {
    public Gson gson = new Gson();
    private ChannelPool channelPool;
    private final static String EXCHANGE_NAME = "swipeExchange";
    private ExecutorService executorService;
    private final int CHANNELS_POOL_SIZE = 60;
    private static final int THREAD_POOL_SIZE = 150;
    private static final int MIN_USER_ID = 1;
    private static final int MAX_SWIPER_ID = 5000;
    private static final int MAX_SWIPEE_ID = 1000000;
    private static final int MAX_COMMENT_LENGTH = 256;
    private final static String QUEUE_NAME_1 = "swipeQueue1";
    private final static String QUEUE_NAME_2 = "swipeQueue2";

    @Override
    public void init() {
        try {
            channelPool = new ChannelPool(CHANNELS_POOL_SIZE);
            executorService = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
        } catch (IOException | TimeoutException e) {
            e.printStackTrace();
        }
    }

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
        PrintWriter out;
        try {
            out = response.getWriter();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if (!validateUrl(urlPath)) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            message.setMessage("Invalid URL");
            resJson = gson.toJson(message);
            out.write(resJson);
            out.flush();
            return;
        }

        String reqBody;
        reqBody = request.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
        SwipeEvent swipe = gson.fromJson(reqBody, SwipeEvent.class);
        if (!validateSwipe(swipe)) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            message.setMessage("Invalid Inputs");
            resJson = gson.toJson(message);
            out.write(resJson);
            out.flush();
            return;
        }

        String leftOrRight = urlPath.replace("/", "");
        swipe.setSwipe(leftOrRight);

        executorService.execute(() -> {
            Channel curChannel;
            try {
                curChannel = channelPool.borrowChannel();
                String jsonMessage = gson.toJson(swipe);
                curChannel.basicPublish("", QUEUE_NAME_1, null, jsonMessage.getBytes());
                curChannel.basicPublish("", QUEUE_NAME_2, null, jsonMessage.getBytes());
//                curChannel.basicPublish(EXCHANGE_NAME, "", null, jsonMessage.getBytes())
//                System.out.println(curChannel.getChannelNumber() + " Sent '" + jsonMessage + "'");
                channelPool.returnChannel(curChannel);
            } catch (Exception e) {
                e.printStackTrace();
                message.setMessage("Create Unsuccessful");
                out.write(gson.toJson(message));
                out.flush();
            }
        });

        response.setStatus(HttpServletResponse.SC_CREATED);
        message.setMessage(("Create Successfully"));
        resJson = gson.toJson(message);
        out.write(resJson);
        out.flush();
        }

    private boolean validateUrl(String url) {
        if (url == null || url.isEmpty()) {
            return false;
        }
        url = url.replace("/", "");
        return url.equals("left") || url.equals("right");
    }

    private boolean validateSwipe(SwipeEvent swipe) {
        if (swipe.getSwiper() == null || swipe.getSwipee() == null || swipe.getComment() == null) {
            return false;
        }
        int swiperId = Integer.parseInt(swipe.getSwiper());
        int swipeeId = Integer.parseInt(swipe.getSwipee());
        return swiperId >= MIN_USER_ID && swiperId <= MAX_SWIPER_ID && swipeeId >= MIN_USER_ID && swipeeId <= MAX_SWIPEE_ID
            && swipe.getComment().length() <= MAX_COMMENT_LENGTH;
    }

    @Override
    public void destroy() {
        super.destroy();
        executorService.shutdown();
        try {
            channelPool.closeChannelPool();
        } catch (InterruptedException | IOException | TimeoutException e) {
            throw new RuntimeException(e);
        }
        try {
            executorService.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
