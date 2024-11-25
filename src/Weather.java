import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.awt.*;
import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Scanner;


public class Weather {
    public static void main(String[] args) {

        Scanner scanner = new Scanner(System.in);
        ObjectMapper mapper = new ObjectMapper();
        String json = null;
        JsonNode jsonNode = null;
        StringBuilder startDateJson = new StringBuilder();
        StringBuilder endDateJson = new StringBuilder();

        System.out.print("Введите желаемую широту: ");
        String lat = scanner.nextLine();
        System.out.print("Введите желаемую долготу: ");
        String lon = scanner.nextLine();
        String weatherURI = "https://api.weather.yandex.ru/v2/forecast?" + "lat=" + lat + "&lon=" + lon;
        String accessKey = getKey("src/key.txt");
        try {
            Object jsonObject = mapper.readValue(
                    getResponse(weatherURI, accessKey), Object.class);
            json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonObject);
        } catch (JsonProcessingException e) {
            System.err.println("Error making prettyJSON: " + e.getMessage());
        }
        System.out.println("Температура в этих кординатах: " + getTemp(json));
        setResponseFile("ResponseBody: " + json);
        try {
            jsonNode = mapper.readTree(json);
        } catch (JsonProcessingException e) {
            System.err.println("Error with finding Dates: " + e.getMessage());
        }
        startDateJson.append(jsonNode != null ? jsonNode.path("forecasts").path(0).get("date").toString() : null);
        startDateJson.deleteCharAt(11);
        startDateJson.deleteCharAt(8);
        startDateJson.deleteCharAt(5);
        startDateJson.deleteCharAt(0);
        endDateJson.append(jsonNode != null ?
                jsonNode.path("forecasts").path(jsonNode.path("forecasts").size() - 1).get("date").toString()
                : null);
        endDateJson.deleteCharAt(11);endDateJson.deleteCharAt(8);endDateJson.deleteCharAt(5);endDateJson.deleteCharAt(0);
        System.out.printf("Выберите даты с %1$s по %2$s в соответствующем формате для вывода информации о " +
                "средней температуре за определенный период. \nДата начала периода: ", startDateJson, endDateJson);
        String startDate = scanner.nextLine();
        System.out.print("Дата окончания периода: ");
        String endDate = scanner.nextLine();
        System.out.printf("Средняя температура в заданный промежуток времени: %.2f",
                getAvgTemp(json, startDate, endDate));
    }

    private static String getKey(String path) {
        StringBuilder key = new StringBuilder();
        try {
            BufferedReader reader = new BufferedReader(new FileReader(path));
            String line;
            while ((line = reader.readLine()) != null) {
                key.append(line);
            }
            reader.close();
        } catch (Exception e) {
            System.err.println("Error with reading accessKey: " + e.getMessage());
        }
        return String.valueOf(key);
    }

    private static String getResponse(String weatherURI, String accessKey) {
        String responseBody = null;
        HttpClient httpClient = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(weatherURI))
                .headers("X-Yandex-Weather-Key", accessKey)
                .GET()
                .build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            //для отладки
            //System.out.println("Response Code: " + response.statusCode());
            responseBody = response.body();
        } catch (Exception e) {
            System.err.println("Error making HTTP request: " + e.getMessage());
        }
        return responseBody;
    }

    private static String getTemp(String jsonString) {
        ObjectMapper objectMapper = new ObjectMapper();
        String temp = "";
        try {
            JsonNode jsonNode = objectMapper.readTree(jsonString);
            temp = jsonNode.path("fact").get("temp").toString();
        } catch (JsonProcessingException e) {
            System.err.println("Error with finding Temp: " + e.getMessage());
        }
        return temp;
    }

    private static double getAvgTemp(String jsonString, String startDate, String endDate) {
        ObjectMapper objectMapper = new ObjectMapper();
        StringBuilder temp;
        int avgTemp = 0;
        int c = 0;
        try {
            JsonNode jsonNode = objectMapper.readTree(jsonString);
            for (int i = 0; i < jsonNode.path("forecasts").size(); i++){
                temp = new StringBuilder(jsonNode.path("forecasts").path(i).get("date").toString());
                temp.deleteCharAt(11);temp.deleteCharAt(8);temp.deleteCharAt(5);temp.deleteCharAt(0);
                if (Integer.parseInt(String.valueOf(temp)) > Integer.parseInt(startDate)
                        && Integer.parseInt(String.valueOf(temp)) < Integer.parseInt(endDate)
                        || Integer.parseInt(String.valueOf(temp)) == Integer.parseInt(startDate)
                        || Integer.parseInt(String.valueOf(temp)) == Integer.parseInt(endDate)){
                    c++;
                    avgTemp += Integer.parseInt(
                            jsonNode.path("forecasts").path(i).path("parts").path("day_short").get("temp").toString());
                }
            }
        } catch (JsonProcessingException e) {
            System.err.println("Error with finding avgTemp: " + e.getMessage());
        }
        return (double) avgTemp /c ;
    }

    private static void setResponseFile(String json) {
        BufferedOutputStream outputStream;
        String fileName = "weatherResponse.txt";
        try {
            outputStream = new BufferedOutputStream(new FileOutputStream(fileName));
            outputStream.write(json.getBytes());
            outputStream.close();
            Desktop.getDesktop().browse(java.net.URI.create(fileName));
        } catch (IOException e) {
            System.err.println("Error with writing file with response: " + e.getMessage());
        }
    }
}