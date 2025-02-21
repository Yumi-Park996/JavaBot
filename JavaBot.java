import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

public class JavaBot {
    public static void main(String[] args) {
        // LLM을 사용하여 알고리즘 추천
        String llmResult = useLLM("자바 알고리즘 중 개발자 현업에서 많이 사용되는 알고리즘을 랜덤으로 하나를 추천하고 설명해주는 내용을 500자 이내로 작성. 별도의 앞뒤 내용 없이 해당 내용만 출력. nutshell, for slack message, in korean.");
        System.out.println("llmResult = " + llmResult);
        sendIssues("Java Algorithm", llmResult);
    }

    public static String useLLM(String prompt) {
        // 환경 변수에서 API URL, API 키, 모델 정보를 가져옴
        String apiUrl = System.getenv("LLM_API_URL");
        String apiKey = System.getenv("LLM_API_KEY");
        String model = System.getenv("LLM_MODEL");

        // 요청 페이로드 생성
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
        
        HttpClient client = HttpClient.newHttpClient(); // HTTP 클라이언트 생성
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl)) // API URL 지정
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                .build();
        
        try {
            HttpResponse<String> response = client.send(request,
                    HttpResponse.BodyHandlers.ofString());
            System.out.println("response.statusCode() = " + response.statusCode());
            System.out.println("response.body() = " + response.body());
            return extractTextFromGeminiResponse(response.body());
        } catch (Exception e) {
            e.printStackTrace(); // 예외 발생 시 스택 트레이스 출력
            throw new RuntimeException(e);
        }
    }

    private static String extractTextFromGeminiResponse(String text) {
        // 응답에서 "text" 필드 값 추출
        int textStart = text.indexOf("\"text\":");
        if (textStart == -1) return "답변을 찾을 수 없음";

        textStart += 8;
        int textEnd = text.indexOf("\"", textStart + 1);
        if (textEnd == -1) return "응답 파싱 오류";

        return text.substring(textStart + 1, textEnd);
    }
    
    public static void sendIssues(String title, String body) {
        // GitHub Issues API를 사용하여 이슈 생성
        String repo = System.getenv("GITHUB_REPO"); // 저장소 이름 ("owner/repo" 형식)
        String token = System.getenv("GITHUB_TOKEN"); // GitHub Personal Access Token
        String apiUrl = "https://api.github.com/repos/" + repo + "/issues";

        // 요청 페이로드 생성
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
                .header("Authorization", "Bearer " + token)
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();

        try {
            HttpResponse<String> response = client.send(request,
                    HttpResponse.BodyHandlers.ofString());
            System.out.println("response.statusCode() = " + response.statusCode());
            System.out.println("response.body() = " + response.body());
        } catch (Exception e) {
            e.printStackTrace(); // 예외 발생 시 스택 트레이스 출력
            throw new RuntimeException(e);
        }
    }
}
