import Model.ChannelPool;
import Model.Message;
import Model.SwipeEvent;
import com.google.gson.Gson;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.*;
import java.io.IOException;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.SystemPropertyCredentialsProvider;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;

@WebServlet(name = "StatsServlet", value = "/StatsServlet")
public class StatsServlet extends HttpServlet {

    public Gson gson = new Gson();
    DynamoDbClient dbClient;

    private final static String tableName = "swipeData";

    @Override
    public void init() {
        AwsCredentialsProvider credentialsProvider = SystemPropertyCredentialsProvider.create();
        System.setProperty("aws.accessKeyId", "ASIAYHSTMVQC7HARPZ72");
        System.setProperty("aws.secretAccessKey", "3ztobe8hZe/TU9xB7RxxqYy+n8uBdh9f1Xmyj6wg");
        System.setProperty("aws.sessionToken",
            "FwoGZXIvYXdzEL3//////////wEaDIZtIEvIZ8Habs/wZiLMARSPqjSYDFBmuswuuy8uVdDDCCrtZPXFe2sYRaHQyw+pXIrU1wqoYlB33vRWE2elMMH1pcdfXFnlPubHKHhCjBM4lZVZTdYL0EAVIqXdGrSYzL1Bf4BlmniGkdjVYlantyukMw+3mZcQSP5nWprgYtgd2SIjBWEcSgdR0jLFN/Y0hwal3DsfMpk5Le/GZHRMB/aFoRkn8Bq9MsYcQOXv5UmWJ/ZSf9Py4S6PPiQUuf4iO5XvKpBZZTo1+TAkd8QDgNZdvu2TSN7gnLN76ij7tJmhBjItIHxbaXnp9nRR623+XRQxQOqYHJwfQbUemLqthwm2aDaUo20+YiufOOnAlJ2Q");

        dbClient = DynamoDbClient.builder()
            .credentialsProvider(credentialsProvider)
            .region(Region.US_WEST_2)
            .build();
    }

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
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

      String swiper = urlPath.split("/")[1];
      response.setStatus(HttpServletResponse.SC_OK);
      GetItemResponse responseItem = getLikesAndDisLikes(swiper, dbClient);
      response.setContentType("application/json");
      if (!responseItem.hasItem()){
          message.setMessage("Item not found");
          resJson = gson.toJson(message);
      } else{
          int likesNum = Integer.parseInt(responseItem.item().get("likes").n());
          int dislikesNum = Integer.parseInt(responseItem.item().get("dislikes").n());
          Map<String, Integer> map = new HashMap<>();
          map.put("likes", likesNum);
          map.put("dislikes", dislikesNum);
          resJson = gson.toJson(map);
      }
      message.setMessage(("Get Successfully"));
      out.write(resJson);
      out.flush();
  }

    private  GetItemResponse getLikesAndDisLikes(String swiper, DynamoDbClient client) {
        Map<String, AttributeValue> keyToGet = new HashMap<>();
        keyToGet.put("swiper", AttributeValue.builder().s(swiper).build());
        GetItemRequest request = GetItemRequest.builder()
            .tableName(tableName)
            .key(keyToGet)
            .build();
        GetItemResponse response = client.getItem(request);
        System.out.println(response.toString());
        return response;
    }

  @Override
  protected void doPost(HttpServletRequest request, HttpServletResponse response) {

  }
    private boolean validateUrl(String url) {
        if (url == null || url.isEmpty()) {
            return false;
        }
        url = url.replace("/", "");
        return url.equals("left") || url.equals("right");
    }
}
