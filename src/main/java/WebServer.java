import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import spark.*;
import weatherman.chatbot.Chatbot;
import weatherman.web.utils.ResponseError;
import weatherman.weather.Weather;

import static spark.Spark.*;
import static weatherman.web.utils.JSONUtil.json;
import static weatherman.web.utils.JSONUtil.toJson;

public class WebServer {

    public static void main(String[] args) {
        Spark.setPort(getHerokuAssignedPort());
        Spark.staticFileLocation("/public");

        Weather w = new Weather();

        final Chatbot bot = new Chatbot(w);

        get("/", (req, res) -> "Hello World! I am WeatherMan, the weather bot!!");

        //post handle for WeatherMan chatbot
        post("/bot", new Route() {
            public Object handle(Request request, Response response) {

                String body = request.body();

                System.out.println("body: " + body);
                String splitChar = "&";
                String keyValueSplitter = "=";
                String[] params = body.split(splitChar);

                String userUtterance = "noneSaid";

                for (int i=0; i < params.length; i++){

                    String[] sv = params[i].split(keyValueSplitter);

                    if (sv[0].equals("userUtterance")){
                        if (sv.length > 0){
                            userUtterance = sv[1];
                        } else {
                            userUtterance = "";
                        }
                        userUtterance = userUtterance.replaceAll("%20", "");
                        userUtterance = userUtterance.replaceAll("%3A", ":");
                    }
                }
                if (!userUtterance.equals("noneSaid")){
                    System.out.println("User says:" + userUtterance);
                    JsonObject userInput = new JsonObject();
                    userInput.add("userUtterance", new JsonPrimitive(userUtterance));
                    String botResponse = bot.process(userInput).toString();
                    System.out.println("Bot says:" + botResponse);
                    if (botResponse != null) {
                        return botResponse;
                    }
                } else {
                    return null;
                }
                response.status(400);
                return new ResponseError("Error! POST not handled.");
            }
        }, json());

        after((req, res) -> { res.type("application/json"); });
        exception(IllegalArgumentException.class, (e, req, res) -> {
            res.status(400);
            res.body(toJson(new ResponseError(e)));
        });
    }


    static int getHerokuAssignedPort() {
        ProcessBuilder processBuilder = new ProcessBuilder();
        if (processBuilder.environment().get("PORT") != null) {
            return
                    Integer.parseInt(processBuilder.environment().get("PORT"));
        }
        return 4567;
    }
}