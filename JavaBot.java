import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JavaBot {
    public static void main(String[] args) {
        // LLM을 사용해 알고리즘 추천 받기
        String llmResult = useLLM("자바 알고리즘 중 개발자 현업에서 많이 사용되는 알고리즘을 랜덤으로 하나를 추천하고 설명해주는 내용을 1000자 이내로 작성. 별도의 앞뒤 내용 없이 해당 내용만 출력. nutshell, for slack message, in korean.");
        System.out.println("llmResult = " + llmResult);
        
        // LLM을 사용해 제목 요약 생성
        String summarizedTitle = summarizeText(llmResult);
        System.out.println("Summarized Title = " + summarizedTitle);

        // GitHub Issue 생성
        sendIssues(summarizedTitle, llmResult);
    }

    public static String useLLM(String prompt) {
        return callLLMApi(prompt);
    }

    public static String summarizeText(String text) {
        // LLM을 이용해 요약 요청
        return callLLMApi("다음 내용을 10자 내외의 간결한 제목으로 요약해줘. '" + text + "'");
    }

    public static String callLLMApi(String prompt) {
        String apiUrl = System.getenv("LLM_API_URL");
        String apiKey = System.getenv("LLM_API_KEY");
        String model = System.getenv("LLM_MODEL");

        String payload = """
                {
                  "messages": [
                    {
                      "role": "user",
                      "content": "%s"
                    }
                  ],
                  "model": "%s"
                }
                """.formatted(prompt, model);

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                .build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            System.out.println("response.statusCode() = " + response.statusCode());
            System.out.println("response.body() = " + response.body());

            if (response.statusCode() == 200) {
                return extractContent(response.body());
            } else {
                return "LLM API 오류: " + response.statusCode();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "예외 발생: " + e.getMessage();
        }
    }

    public static String extractContent(String json) {
        // 정규식: `"content":"여기에 내용"`
        Pattern pattern = Pattern.compile("\"content\":\"([^\"]+)\"");
        Matcher matcher = pattern.matcher(json);

        if (matcher.find()) {
            return matcher.group(1); // 첫 번째 그룹(내용) 반환
        }
        return "응답에서 content 값을 찾을 수 없음";
    }

    
    public static void sendIssues(String title, String body) {
        String repo = System.getenv("GITHUB_REPO");
        String token = System.getenv("GITHUB_TOKEN");
        String apiUrl = "https://api.github.com/repos/" + repo + "/issues";

        if (repo == null || token == null) {
            System.out.println("GITHUB_REPO 또는 GITHUB_TOKEN 환경 변수가 설정되지 않았습니다.");
            return;
        }

        String payload = """
                {
                  "title": "%s",
                  "body": "%s"
                }
                """.formatted(title, body);

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .header("Content-Type", "application/json")
                .header("Accept", "application/vnd.github.v3+json")
                .header("Authorization", "Bearer " + token)
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            System.out.println("response.statusCode() = " + response.statusCode());
            System.out.println("response.body() = " + response.body());

            if (response.statusCode() == 201) {
                System.out.println("✅ GitHub Issue 생성 성공!");
            } else {
                System.out.println("❌ GitHub Issue 생성 실패: " + response.body());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}