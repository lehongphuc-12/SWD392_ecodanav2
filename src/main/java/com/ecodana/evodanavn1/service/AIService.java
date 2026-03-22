package com.ecodana.evodanavn1.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class AIService {

    @Value("${ai.cloudflare.account-id}")
    private String accountId;

    @Value("${ai.cloudflare.api-token}")
    private String apiToken;

    @Value("${ai.cloudflare.model:@cf/meta/llama-3-8b-instruct}")
    private String model;

    private static final Logger logger = LoggerFactory.getLogger(AIService.class);
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    // ===== PROMPT GỐC CỐ ĐỊNH =====
    private static final String SYSTEM_PROMPT = """
            ## Ngữ cảnh
            Hôm nay là ngày: {current_date}.
            Bạn là **trợ lý ảo EcoDana**, chuyên về **dịch vụ thuê xe điện** (ô tô điện, xe máy điện).
            Luôn trả lời bằng **Tiếng Việt**, **ngắn gọn**, **thân thiện**.

            ## 🎯 Nhiệm vụ
            Dựa vào yêu cầu của người dùng, bạn phải chọn MỘT trong ba hành động sau:

            ## ⚙️ Quy tắc đặc biệt

            **1️⃣. KHI NGƯỜI DÙNG TÌM XE (QUAN TRỌNG NHẤT):**
            - Nếu người dùng muốn tìm xe với các tiêu chí (số chỗ, loại xe, ngày, giá...), **CHỈ** trả về một chuỗi JSON duy nhất.
            - **Định dạng JSON:** `{"intent": "search", "filters": {...}}`
            - **QUAN TRỌNG:** Chỉ trích xuất các tiêu chí có trong lời nói của người dùng. KHÔNG được tự ý thêm, suy diễn hoặc giả định bất kỳ tiêu chí nào không được cung cấp.
            - **Các `filters` hợp lệ (dùng camelCase):**
              - `type`: 'ElectricCar' (ô tô điện), 'ElectricMotorcycle' (xe máy điện).
              - `seats`: số chỗ ngồi (số nguyên).
              - `budget`: giá tối đa (số nguyên, ví dụ: 500000).
              - `pickupDate`, `returnDate`: 'YYYY-MM-DD'.
            - **Ví dụ tìm xe:**
              - "tìm xe 4 chỗ" → `{"intent":"search","filters":{"seats":4,"type":"ElectricCar"}}`
              - "xe máy điện giá dưới 200k" → `{"intent":"search","filters":{"type":"ElectricMotorcycle","budget":200000}}`
              - "thuê xe 7 chỗ từ ngày mai" → `{"intent":"search","filters":{"seats":7,"pickupDate":"{tomorrow_date}"}}`
              - "cần xe 2 chỗ cuối tuần này" → `{"intent":"search","filters":{"seats":2,"pickupDate":"{saturday_date}","returnDate":"{sunday_date}"}}`

            **2️⃣. KHI NGƯỜI DÙNG MUỐN ĐIỀU HƯỚNG HOẶC CẦN TƯ VẤN:**
            - Nếu người dùng muốn đến các trang chung chung (trang chủ, liên hệ, bảng giá) hoặc hỏi những câu cần tư vấn chuyên sâu, trả về JSON với `intent: "redirect"`.
            - **QUAN TRỌNG:** Các câu hỏi về chính sách, thông tin công ty, hoặc các vấn đề phức tạp nên được chuyển hướng đến trang `/contact`.
            - **Ví dụ điều hướng:**
              - "quay về trang chủ" → `{"intent":"redirect","url":"/"}`
              - "tôi cần liên hệ" hoặc "làm sao để gặp nhân viên?" → `{"intent":"redirect","url":"/contact"}`
              - "cho tôi xem tất cả xe" hoặc "bảng giá" → `{"intent":"redirect","url":"/vehicles"}`
              - "xe của hãng nào?" hoặc "chính sách bảo hành ra sao?" → `{"intent":"redirect","url":"/contact"}`

            **3️⃣. KHI NGƯỜI DÙNG HỎI CÂU ĐƠN GIẢN:**
            - Chỉ trả lời bằng văn bản cho những câu hỏi rất đơn giản và phổ biến.
            - **Ví dụ trả lời:**
              - "thủ tục thuê xe thế nào?" → "Thủ tục thuê xe rất đơn giản, chỉ cần CCCD hoặc bằng lái xe hợp lệ là được bạn nhé."
              - "EcoDana ở đâu?" → "Hiện tại chúng tôi chủ yếu hoạt động tại Đà Nẵng bạn nhé."
            """;

    // Hàm tạo HTML link khi intent là search
    private String buildSearchResponse(Map<String, Object> filters) {
        List<String> query = new ArrayList<>();
        List<String> desc = new ArrayList<>();

        if (filters.containsKey("seats")) {
            query.add("seats=" + filters.get("seats"));
            desc.add(filters.get("seats") + " chỗ");
        }
        if (filters.containsKey("type")) {
            String type = (String) filters.get("type");
            query.add("type=" + type);
            desc.add(type.equals("ElectricCar") ? "ô tô điện" : "xe máy điện");
        }
        if (filters.containsKey("budget")) {
            query.add("budget=" + filters.get("budget"));
            desc.add("giá dưới " + String.format("%,d", filters.get("budget")) + "đ");
        }
        if (filters.containsKey("pickupDate")) {
            query.add("pickupDate=" + filters.get("pickupDate"));
            desc.add("nhận từ ngày " + filters.get("pickupDate"));
        }
        if (filters.containsKey("returnDate")) {
            query.add("returnDate=" + filters.get("returnDate"));
            desc.add("trả trước ngày " + filters.get("returnDate"));
        }

        if (desc.isEmpty()) {
            return "Đã tìm thấy các xe phù hợp. Bạn có thể <a href='/vehicles' style='color:#007bff;text-decoration:underline;font-weight:bold;'>xem tại đây</a>.";
        }

        String url = "/vehicles?" + String.join("&", query);
        String description = "Đã tìm thấy xe " + String.join(" và ", desc) + ". Bạn có thể ";
        String linkHtml = "<a href='" + url + "' style='color:#007bff;text-decoration:underline;font-weight:bold;'>xem tại đây</a>.";
        return "<p>" + description + linkHtml + "</p>";
    }

    public String askAI(String message) {
        if (accountId == null || apiToken == null || accountId.isBlank() || apiToken.isBlank()) {
            logger.error("❌ Cloudflare Account ID hoặc API Token chưa được cấu hình.");
            return "⚠️ Dịch vụ AI chưa được cấu hình. Vui lòng liên hệ quản trị viên.";
        }

        String url = "https://api.cloudflare.com/client/v4/accounts/" + accountId + "/ai/run/" + model;

        LocalDate today = LocalDate.now();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String currentDate = today.format(fmt);

        String systemPrompt = SYSTEM_PROMPT.replace("{current_date}", currentDate);

        try {
            // ==== Chuẩn bị body ====
            Map<String, Object> body = new HashMap<>();
            List<Map<String, String>> messages = new ArrayList<>();
            messages.add(Map.of("role", "system", "content", systemPrompt));
            messages.add(Map.of("role", "user", "content", message));
            body.put("messages", messages);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiToken);

            HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(body), headers);

            // ==== Gọi API Cloudflare AI ====
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                logger.error("❌ Cloudflare AI lỗi: {} - {}", response.getStatusCode(), response.getBody());
                return "⚠️ Lỗi khi gọi Cloudflare AI (" + response.getStatusCode() + ").";
            }

            // ==== Xử lý phản hồi ====
            Map<String, Object> json = objectMapper.readValue(response.getBody(), Map.class);
            Map<String, Object> result = (Map<String, Object>) json.get("result");
            String raw = (String) result.get("response");

            // Thay placeholder ngày
            raw = raw.replace("{tomorrow_date}", today.plusDays(1).format(fmt))
                    .replace("{saturday_date}", today.with(java.time.DayOfWeek.SATURDAY).format(fmt))
                    .replace("{sunday_date}", today.with(java.time.DayOfWeek.SUNDAY).format(fmt))
                    .replace("{year}", String.valueOf(today.getYear()));

            // ==== Cải tiến: Tìm và xử lý JSON redirect một cách linh hoạt ====
            // AI có thể trả về JSON trong một khối mã ```json ... ```
            Pattern jsonPattern = Pattern.compile("\\{.*\"intent\".*\\}", Pattern.DOTALL);
            Matcher matcher = jsonPattern.matcher(raw);

            if (matcher.find()) {
                String jsonString = matcher.group();
                try {
                    Map<String, Object> obj = objectMapper.readValue(jsonString, Map.class);
                    String intent = (String) obj.get("intent");

                    if ("search".equals(intent) && obj.containsKey("filters")) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> filters = (Map<String, Object>) obj.get("filters");
                        return buildSearchResponse(filters);
                    }

                    if ("redirect".equals(intent) && obj.containsKey("url")) {
                        String redirectUrl = (String) obj.get("url");
                        String responseMessage;
                        if (redirectUrl.contains("/contact")) {
                            responseMessage = "Để được tư vấn chi tiết hơn, bạn vui lòng ";
                        } else if (redirectUrl.equals("/")) {
                            responseMessage = "Đang chuyển bạn về trang chủ. Bạn có thể ";
                        } else {
                            responseMessage = "Chắc chắn rồi! Bạn có thể ";
                        }
                        String linkHtml = "<a href='" + redirectUrl + "' style='color:#007bff;text-decoration:underline;font-weight:bold;'>xem tại đây</a>.";
                        return "<p>" + responseMessage + linkHtml + "</p>";
                    }

                } catch (Exception e) {
                    logger.warn("⚠️ Không parse được JSON redirect từ AI: {}", jsonString, e);
                    // Nếu parse lỗi, sẽ đi đến logic fallback bên dưới
                }
            }

            // Fallback: Nếu AI trả về văn bản thường (theo quy tắc 3) hoặc không tìm thấy JSON hợp lệ
            // *** SỬA LỖI LOGIC FALLBACK ***
            // Chỉ trả về thông báo lỗi nếu câu trả lời của AI trống hoặc quá ngắn, thay vì kiểm tra thẻ HTML.
            if (raw == null || raw.isBlank() || raw.length() < 10) {
                logger.warn("AI không trả về JSON hoặc HTML, câu hỏi có thể chưa được xử lý: '{}'", message);
                return "Xin lỗi, tôi chưa hiểu rõ câu hỏi của bạn. Để được hỗ trợ tốt nhất, bạn vui lòng <a href='/contact' style='color:#007bff;text-decoration:underline;font-weight:bold;'>liên hệ trực tiếp</a> với chúng tôi nhé.";
            }

            return raw;

        } catch (HttpClientErrorException e) {
            logger.error("❌ Lỗi Cloudflare AI: {} - {}", e.getStatusCode(), e.getResponseBodyAsString(), e);
            return "⚠️ Đã xảy ra lỗi khi kết nối với dịch vụ AI. Vui lòng thử lại sau.";
        } catch (Exception e) {
            logger.error("❌ Lỗi không xác định khi gọi Cloudflare AI", e);
            return "⚠️ Có lỗi không xác định xảy ra. Vui lòng thử lại sau.";
        }
    }
}
